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

import java.lang.ref.Reference;
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
 * that displays a portal page (a portal session). An instance 
 * is created when a new portal window opens the websocket 
 * connection to the server for the first time. If the 
 * connection between the browser and the server is lost, 
 * the portal code in the browser tries to establish a 
 * new websocket connection to the same, already
 * existing {@link ConsoleSession}. The {@link ConsoleSession} 
 * object is thus independent of the websocket connection 
 * that handles the actual transfer of notifications.
 * 
 * ![WebConsole Session](ConsoleSession.svg)
 * 
 * Because there is no reliable way to be notified when a
 * window in a browser closes, {@link ConsoleSession}s are
 * discarded automatically unless {@link #refresh()} is called
 * before the timeout occurs.
 * 
 * {@link ConsoleSession} implements the {@link IOSubchannel}
 * interface. This allows the instances to be used as channels
 * for exchanging portal session scoped events with the 
 * {@link WebConsole} component. The upstream channel
 * (see {@link #upstreamChannel()}) is the channel of the
 * WebSocket. It may be unavailable if the connection has
 * been interrupted and not (yet) re-established.
 * 
 * The response {@link EventPipeline} must be used to send
 * events (responses) to the portal session in the browser.
 * No other event pipeline may be used for this purpose, else
 * messages will interleave.
 * 
 * Because a portal session (channel) remains conceptually open
 * even if the connection to the browser is lost (until the
 * timeout occurs) {@link Closed} events from upstream are not
 * forwarded immediately. Rather, a {@link Closed} event is
 * fired on the channel when the timeout occurs. Method
 * {@link #isConnected()} can be used to check the connection 
 * state.
 *  
 * As a convenience, the {@link ConsoleSession} provides
 * direct access to the browser session, which can 
 * usually only be obtained from the HTTP event or WebSocket
 * channel by looking for an association of type {@link Session}.
 * 
 * @startuml ConsoleSession.svg
 * class ConsoleSession {
 *  -{static}Map<String,ConsoleSession> portalSessions
 *  +{static}findOrCreate(String portalSessionId, Manager component): ConsoleSession
 *  +setTimeout(timeout: long): ConsoleSession
 *  +refresh(): void
 *  +setUpstreamChannel(IOSubchannel upstreamChannel): ConsoleSession
 *  +setSession(Session browserSession): ConsoleSession
 *  +upstreamChannel(): Optional<IOSubchannel>
 *  +portalSessionId(): String
 *  +browserSession(): Session
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
public final class ConsoleSession extends DefaultIOSubchannel {

    private static Map<String, WeakReference<ConsoleSession>> portalSessions
        = new ConcurrentHashMap<>();
    private static ReferenceQueue<ConsoleSession> unusedSessions
        = new ReferenceQueue<>();

    private String portalSessionId;
    private final WebConsole portal;
    private final Set<Locale> supportedLocales;
    private Locale locale;
    private long timeout;
    private final Timer timeoutTimer;
    private boolean active = true;
    private boolean connected = true;
    private Session browserSession;
    private IOSubchannel upstreamChannel;

    private static void cleanUnused() {
        while (true) {
            Reference<? extends ConsoleSession> unused = unusedSessions.poll();
            if (unused == null) {
                break;
            }
            portalSessions.remove(unused.get().portalSessionId());
        }
    }

    /**
     * Lookup the portal session (the channel)
     * for the given portal session id.
     * 
     * @param portalSessionId the portal session id
     * @return the channel
     */
    /* default */ static Optional<ConsoleSession>
            lookup(String portalSessionId) {
        cleanUnused();
        return Optional.ofNullable(portalSessions.get(portalSessionId))
            .flatMap(ref -> Optional.ofNullable(ref.get()));
    }

    /**
     * Return all sessions that belong to the given portal as a new
     * unmodifiable set.
     *
     * @param portal the portal
     * @return the sets the
     */
    public static Set<ConsoleSession> byPortal(WebConsole portal) {
        cleanUnused();
        Set<ConsoleSession> result = new HashSet<>();
        for (WeakReference<ConsoleSession> psr : portalSessions.values()) {
            ConsoleSession psess = psr.get();
            if (psess.portal.equals(portal)) {
                result.add(psess);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Lookup (and create if not found) the portal browserSession channel
     * for the given portal browserSession id.
     *
     * @param portalSessionId the browserSession id
     * @param portal the portal that this session belongs to
     * class' constructor if a new channel is created, usually 
     * the portal
     * @param supportedLocales the locales supported by the portal
     * @param timeout the portal session timeout in milli seconds
     * @return the channel
     */
    /* default */ static ConsoleSession lookupOrCreate(
            String portalSessionId, WebConsole portal,
            Set<Locale> supportedLocales, long timeout) {
        cleanUnused();
        return portalSessions.computeIfAbsent(portalSessionId,
            psi -> new WeakReference<>(new ConsoleSession(
                portal, supportedLocales, portalSessionId, timeout),
                unusedSessions))
            .get();
    }

    /**
     * Replace the id of the portal session with the new id.
     *
     * @param newPortalSessionId the new portal session id
     * @return the portal session
     */
    /* default */ ConsoleSession replaceId(String newPortalSessionId) {
        portalSessions.remove(portalSessionId);
        portalSessionId = newPortalSessionId;
        portalSessions.put(portalSessionId, new WeakReference<>(
            this, unusedSessions));
        connected = true;
        return this;
    }

    private ConsoleSession(WebConsole portal, Set<Locale> supportedLocales,
            String portalSessionId, long timeout) {
        super(portal.channel(), portal.newEventPipeline());
        this.portal = portal;
        this.supportedLocales = supportedLocales;
        this.portalSessionId = portalSessionId;
        this.timeout = timeout;
        timeoutTimer = Components.schedule(
            tmr -> discard(), Duration.ofMillis(timeout));
    }

    /**
     * Changes the timeout for this {@link ConsoleSession} to the
     * given value.
     * 
     * @param timeout the timeout in milli seconds
     * @return the portal session for easy chaining
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
        portalSessions.remove(portalSessionId);
        connected = false;
        active = false;
        portal.newEventPipeline().fire(new Closed(), this);
    }

    /**
     * Checks if the portal session has become stale (inactive).
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
     * Sets or updates the upstream channel. This method should only
     * be invoked by the creator of the {@link ConsoleSession}, by default
     * the {@link ConsoleWeblet}.
     * 
     * @param upstreamChannel the upstream channel (WebSocket connection)
     * @return the portal session for easy chaining
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
     * @param browserSession the browser session
     * @return the portal session for easy chaining
     */
    public ConsoleSession setSession(Session browserSession) {
        this.browserSession = browserSession;
        if (locale == null) {
            locale = browserSession.locale();
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
     * The portal session id is used in the communication between the
     * browser and the server. It is not guaranteed to remain the same
     * over time, even if the portal session is maintained. To prevent
     * wrong usage, its visibility is therefore set to package. 
     * 
     * @return the portalSessionId
     */
    /* default */ String portalSessionId() {
        return portalSessionId;
    }

    /**
     * @return the browserSession
     */
    public Session browserSession() {
        return browserSession;
    }

    /**
     * Returns the supported locales.
     *
     * @return the set of locales supported by the portal
     */
    public Set<Locale> supportedLocales() {
        return supportedLocales;
    }

    /**
     * Return the portal session's locale. The locale is initialized
     * from the browser session's locale.
     * 
     * @return the locale
     */
    public Locale locale() {
        return locale == null ? Locale.getDefault() : locale;
    }

    /**
     * Sets the locale for this portal session.
     *
     * @param locale the locale
     * @return the portal session
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
