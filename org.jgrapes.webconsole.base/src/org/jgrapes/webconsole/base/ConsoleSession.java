/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.base;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Subchannel;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultIOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.util.LinkedIOSubchannel;

/**
 * The server side representation of a window in the browser 
 * that displays a console page (a console session). An instance 
 * is created when a new console window opens the WebSocket 
 * connection to the server for the first time. If the 
 * connection between the browser and the server is lost
 * (e.g. due to temporary network failure), the console code in 
 * the browser tries to establish a new WebSocket connection 
 * to the same, already existing {@link ConsoleSession}. 
 * The {@link ConsoleSession} object is thus independent 
 * of the WebSocket connection that handles the actual transfer 
 * of notifications.
 * 
 * ![WebConsole Session](ConsoleSession.svg)
 * 
 * To allow reconnection and because there is no reliable way 
 * to be notified when a window in a browser closes, {@link Closed} 
 * events from the network are ignored and {@link ConsoleSession}s 
 * remain in an open state as long as they are in use. Only if no 
 * data is received (i.e. {@link #refresh()} isn't called) for the 
 * time span configured with
 * {@link ConsoleWeblet#setConsoleSessionNetworkTimeout}
 * a {@link Closed} event is fired on the channel and the console 
 * session is closed. (Method {@link #isConnected()} can be used 
 * to check the connection state.)
 * In order to keep the console connection in an open state while
 * the session is inactive, i.e. no data is sent due to user activity,
 * the SPA automatically generates refresh messages as configured
 * with {@link ConsoleWeblet#setConsoleSessionRefreshInterval(Duration)}. 
 * 
 * {@link ConsoleSession} implements the {@link IOSubchannel}
 * interface. This allows the instances to be used as channels
 * for exchanging console session scoped events with the 
 * {@link WebConsole} component. The upstream channel
 * (see {@link #upstreamChannel()}) is the channel of the
 * WebSocket. It may be unavailable if the connection has
 * been interrupted and not (yet) re-established.
 * The {@link IOSubchannel}'s response {@link EventPipeline} 
 * must be used to send events (responses) to the console session 
 * in the browser. No other event pipeline may be used for 
 * this purpose, else messages will interleave.
 * 
 * To avoid having too many open WebSockets with inactive sessions,
 * a maximum inactivity time can be configured with 
 * {@link ConsoleWeblet#setConsoleSessionInactivityTimeout(Duration)}.
 * The SPA always checks if the time since the last user activity 
 * has reached or exceeded the configured limit before sending the 
 * next refresh message. In case it has, the SPA stops sending 
 * refresh messages and displays a "suspended" dialog to the user.
 * 
 * When the user chooses to resume, a new WebSocket is opened by the
 * SPA. If the {@link Session} used before the idle timeout is 
 * still available (hasn't reached its idle timeout or absolute timeout)
 * and refers to a {@link ConsoleSession} not yet closed, then this
 * {@link ConsoleSession} is reused, else the SPA is reloaded.
 * 
 * As a convenience, the {@link ConsoleSession} provides
 * direct access to the browser session, which can 
 * usually only be obtained from the HTTP event or WebSocket
 * channel by looking for an association of type {@link Session}.
 * 
 * @startuml ConsoleSession.svg
 * class ConsoleSession {
 *  -{static}Map<String,ConsoleSession> consoleSessions
 *  +{static}findOrCreate(String consoleSessionId, Manager component): ConsoleSession
 *  +setTimeout(timeout: long): ConsoleSession
 *  +refresh(): void
 *  +setUpstreamChannel(IOSubchannel upstreamChannel): ConsoleSession
 *  +setSessionSupplier(Session browserSessionSupplier): ConsoleSession
 *  +upstreamChannel(): Optional<IOSubchannel>
 *  +consoleSessionId(): String
 *  +browserSession(): Optional<Session>
 *  +locale(): Locale
 *  +setLocale(Locale locale): void
 * }
 * Interface IOSubchannel {
 * }
 * IOSubchannel <|.. ConsoleSession
 *
 * ConsoleSession "1" *-- "*" ConsoleSession : maintains
 * 
 * package org.jgrapes.http {
 *     class Session
 * }
 * 
 * ConsoleSession "*" -up-> "1" Session: browser session
 * @enduml
 */
@SuppressWarnings("PMD.LinguisticNaming")
public final class ConsoleSession extends DefaultIOSubchannel {

    private static Map<String, WeakReference<ConsoleSession>> consoleSessions
        = new ConcurrentHashMap<>();
    private static ReferenceQueue<ConsoleSession> unusedSessions
        = new ReferenceQueue<>();

    private String consoleSessionId;
    private final WebConsole console;
    private final Set<Locale> supportedLocales;
    private Locale locale;
    private long timeout;
    private final Timer timeoutTimer;
    private boolean active = true;
    private boolean connected = true;
    private Supplier<Optional<Session>> browserSessionSupplier;
    private IOSubchannel upstreamChannel;

    /**
     * Weak reference to session.
     */
    private static class SessionReference
            extends WeakReference<ConsoleSession> {

        private final String id;

        /**
         * Instantiates a new session reference.
         *
         * @param referent the referent
         */
        public SessionReference(ConsoleSession referent) {
            super(referent, unusedSessions);
            id = referent.consoleSessionId();
        }
    }

    private static void cleanUnused() {
        while (true) {
            SessionReference unused = (SessionReference) unusedSessions.poll();
            if (unused == null) {
                break;
            }
            consoleSessions.remove(unused.id);
        }
    }

    /**
     * Lookup the console session (the channel)
     * for the given console session id.
     * 
     * @param consoleSessionId the console session id
     * @return the channel
     */
    /* default */ static Optional<ConsoleSession>
            lookup(String consoleSessionId) {
        cleanUnused();
        return Optional.ofNullable(consoleSessions.get(consoleSessionId))
            .flatMap(ref -> Optional.ofNullable(ref.get()));
    }

    /**
     * Return all sessions that belong to the given console as a new
     * unmodifiable set.
     *
     * @param console the console
     * @return the sets the
     */
    public static Set<ConsoleSession> byConsole(WebConsole console) {
        cleanUnused();
        Set<ConsoleSession> result = new HashSet<>();
        for (WeakReference<ConsoleSession> psr : consoleSessions.values()) {
            ConsoleSession psess = psr.get();
            if (psess != null && psess.console != null
                && psess.console.equals(console)) {
                result.add(psess);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Lookup (and create if not found) the console browserSessionSupplier channel
     * for the given console browserSessionSupplier id.
     *
     * @param consoleSessionId the browserSessionSupplier id
     * @param console the console that this session belongs to
     * class' constructor if a new channel is created, usually 
     * the console
     * @param supportedLocales the locales supported by the console
     * @param timeout the console session timeout in milli seconds
     * @return the channel
     */
    /* default */ static ConsoleSession lookupOrCreate(
            String consoleSessionId, WebConsole console,
            Set<Locale> supportedLocales, long timeout) {
        cleanUnused();
        return consoleSessions.computeIfAbsent(consoleSessionId,
            psi -> new SessionReference(new ConsoleSession(
                console, supportedLocales, consoleSessionId, timeout)))
            .get();
    }

    /**
     * Replace the id of the console session with the new id.
     *
     * @param newConsoleSessionId the new console session id
     * @return the console session
     */
    /* default */ ConsoleSession replaceId(String newConsoleSessionId) {
        consoleSessions.remove(consoleSessionId);
        consoleSessionId = newConsoleSessionId;
        consoleSessions.put(consoleSessionId, new SessionReference(
            this));
        connected = true;
        return this;
    }

    private ConsoleSession(WebConsole console, Set<Locale> supportedLocales,
            String consoleSessionId, long timeout) {
        super(console.channel(), console.newEventPipeline());
        this.console = console;
        this.supportedLocales = supportedLocales;
        this.consoleSessionId = consoleSessionId;
        this.timeout = timeout;
        timeoutTimer = Components.schedule(
            tmr -> discard(), Duration.ofMillis(timeout));
    }

    /**
     * Changes the timeout for this {@link ConsoleSession} to the
     * given value.
     * 
     * @param timeout the timeout in milli seconds
     * @return the console session for easy chaining
     */
    public ConsoleSession setTimeout(long timeout) {
        this.timeout = timeout;
        timeoutTimer.reschedule(Duration.ofMillis(timeout));
        return this;
    }

    /**
     * Returns the time when this session will expire.
     *
     * @return the instant
     */
    public Instant expiresAt() {
        return timeoutTimer.scheduledFor();
    }

    /**
     * Resets the {@link ConsoleSession}'s timeout.
     */
    public void refresh() {
        timeoutTimer.reschedule(Duration.ofMillis(timeout));
    }

    /**
     * Discards this session.
     */
    public void discard() {
        consoleSessions.remove(consoleSessionId);
        connected = false;
        active = false;
        console.newEventPipeline().fire(new Closed(), this);
    }

    /**
     * Checks if the console session has become stale (inactive).
     *
     * @return true, if is stale
     */
    public boolean isStale() {
        return !active;
    }

    /* default */ void disconnected() {
        connected = false;
    }

    /**
     * Checks if a network connection with the browser exists.
     *
     * @return true, if is connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Provides access to the weblet's channel.
     *
     * @return the channel
     */
    public Channel webletChannel() {
        return console.webletChannel();
    }

    /**
     * Sets or updates the upstream channel. This method should only
     * be invoked by the creator of the {@link ConsoleSession}, by default
     * the {@link ConsoleWeblet}.
     * 
     * @param upstreamChannel the upstream channel (WebSocket connection)
     * @return the console session for easy chaining
     */
    public ConsoleSession setUpstreamChannel(IOSubchannel upstreamChannel) {
        if (upstreamChannel == null) {
            throw new IllegalArgumentException();
        }
        this.upstreamChannel = upstreamChannel;
        return this;
    }

    /**
     * Sets or updates associated browser session. This method should only
     * be invoked by the creator of the {@link ConsoleSession}, by default
     * the {@link ConsoleWeblet}.
     * 
     * @param sessionSupplier the browser session supplier
     * @return the console session for easy chaining
     */
    public ConsoleSession
            setSessionSupplier(Supplier<Optional<Session>> sessionSupplier) {
        this.browserSessionSupplier = sessionSupplier;
        if (locale == null) {
            locale = browserSession().locale();
        }
        return this;
    }

    /**
     * @return the upstream channel
     */
    public IOSubchannel upstreamChannel() {
        return upstreamChannel;
    }

    /**
     * The console session id is used in the communication between the
     * browser and the server. It is not guaranteed to remain the same
     * over time, even if the console session is maintained. To prevent
     * wrong usage, its visibility is therefore set to package. 
     * 
     * @return the consoleSessionId
     */
    /* default */ String consoleSessionId() {
        return consoleSessionId;
    }

    /**
     * @return the browserSession
     */
    public Session browserSession() {
        return browserSessionSupplier.get().get();
    }

    /**
     * Returns the supported locales.
     *
     * @return the set of locales supported by the console
     */
    public Set<Locale> supportedLocales() {
        return supportedLocales;
    }

    /**
     * Return the console session's locale. The locale is initialized
     * from the browser session's locale.
     * 
     * @return the locale
     */
    public Locale locale() {
        return locale == null ? Locale.getDefault() : locale;
    }

    /**
     * Sets the locale for this console session.
     *
     * @param locale the locale
     * @return the console session
     */
    public ConsoleSession setLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Subchannel.toString(this));
        Optional.ofNullable(upstreamChannel).ifPresent(upstr -> builder.append(
            LinkedIOSubchannel.upstreamToString(upstr)));
        return builder.toString();
    }

}
