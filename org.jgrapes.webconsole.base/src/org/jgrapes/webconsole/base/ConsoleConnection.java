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
 * that displays a console page (a console connection). An instance 
 * is created when a new console window opens the WebSocket 
 * connection to the server for the first time. If the 
 * connection between the browser and the server is lost
 * (e.g. due to temporary network failure), the console code in 
 * the browser tries to establish a new WebSocket connection 
 * to the same, already existing {@link ConsoleConnection}. 
 * The {@link ConsoleConnection} object is thus independent 
 * of the WebSocket connection that handles the actual transfer 
 * of notifications.
 * 
 * ![WebConsole Connection](ConsoleConnection.svg)
 * 
 * To allow reconnection and because there is no reliable way 
 * to be notified when a window in a browser closes, {@link Closed} 
 * events from the network are ignored and {@link ConsoleConnection}s 
 * remain in an open state as long as they are in use. Only if no 
 * data is received (i.e. {@link #refresh()} isn't called) for the 
 * time span configured with
 * {@link ConsoleWeblet#setConnectionNetworkTimeout}
 * a {@link Closed} event is fired on the channel and the console 
 * connection is closed. (Method {@link #isConnected()} can be used 
 * to check the connection state.)
 * In order to keep the console connection in an open state while
 * the connection is inactive, i.e. no data is sent due to user activity,
 * the SPA automatically generates refresh messages as configured
 * with {@link ConsoleWeblet#setConnectionRefreshInterval(Duration)}. 
 * 
 * {@link ConsoleConnection} implements the {@link IOSubchannel}
 * interface. This allows the instances to be used as channels
 * for exchanging console connection scoped events with the 
 * {@link WebConsole} component. The upstream channel
 * (see {@link #upstreamChannel()}) is the channel of the
 * WebSocket. It may be unavailable if the WebSocket connection has
 * been interrupted and not (yet) re-established.
 * The {@link IOSubchannel}'s response {@link EventPipeline} 
 * must be used to send events (responses) to the console connection 
 * in the browser. No other event pipeline may be used for 
 * this purpose, else messages will interleave.
 * 
 * To avoid having too many open WebSockets with inactive console
 * connections, a maximum inactivity time can be configured with 
 * {@link ConsoleWeblet#setConnectionInactivityTimeout(Duration)}.
 * The SPA always checks if the time since the last user activity 
 * has reached or exceeded the configured limit before sending the 
 * next refresh message. In case it has, the SPA stops sending 
 * refresh messages and displays a "suspended" dialog to the user.
 * 
 * When the user chooses to resume, a new WebSocket is opened by the
 * SPA. If the {@link Session} used before the idle timeout is 
 * still available (hasn't reached its idle timeout or absolute timeout)
 * and refers to a {@link ConsoleConnection} not yet closed, then this
 * {@link ConsoleConnection} is reused, else the SPA is reloaded.
 * 
 * As a convenience, the {@link ConsoleConnection} provides
 * direct access to the browser session, which can 
 * usually only be obtained from the HTTP event or WebSocket
 * channel by looking for an association of type {@link Session}.
 * 
 * @startuml ConsoleConnection.svg
 * class ConsoleConnection {
 *  -{static}Map<String,ConsoleConnection> consoleConnections
 *  +{static}findOrCreate(String consoleConnectionId, Manager component): ConsoleConnection
 *  +setTimeout(timeout: long): ConsoleConnection
 *  +refresh(): void
 *  +setUpstreamChannel(IOSubchannel upstreamChannel): ConsoleConnection
 *  +setSessionSupplier(Session sessionSupplier): ConsoleConnection
 *  +upstreamChannel(): Optional<IOSubchannel>
 *  +consoleConnectionId(): String
 *  +session(): Optional<Session>
 *  +locale(): Locale
 *  +setLocale(Locale locale): void
 * }
 * 
 * interface IOSubchannel {
 * }
 * 
 * IOSubchannel <|.. ConsoleConnection
 * 
 * ConsoleConnection "1" *-- "*" ConsoleConnection : maintains
 * 
 * package org.jgrapes.http {
 *     class Session
 * }
 * 
 * ConsoleConnection "*" -up-> "1" Session: browser session
 * @enduml
 */
@SuppressWarnings("PMD.LinguisticNaming")
public final class ConsoleConnection extends DefaultIOSubchannel {

    private static Map<String, WeakReference<ConsoleConnection>> connections
        = new ConcurrentHashMap<>();
    private static ReferenceQueue<ConsoleConnection> unusedConnections
        = new ReferenceQueue<>();

    private String connectionId;
    private final WebConsole console;
    private final Set<Locale> supportedLocales;
    private Locale locale;
    private long timeout;
    private final Timer timeoutTimer;
    private boolean active = true;
    private boolean connected = true;
    private Supplier<Optional<Session>> sessionSupplier;
    private IOSubchannel upstreamChannel;

    /**
     * Weak reference to session.
     */
    private static class ConnectionReference
            extends WeakReference<ConsoleConnection> {

        private final String id;

        /**
         * Instantiates a new session reference.
         *
         * @param referent the referent
         */
        public ConnectionReference(ConsoleConnection referent) {
            super(referent, unusedConnections);
            id = referent.consoleConnectionId();
        }
    }

    private static void cleanUnused() {
        while (true) {
            ConnectionReference unused
                = (ConnectionReference) unusedConnections.poll();
            if (unused == null) {
                break;
            }
            connections.remove(unused.id);
        }
    }

    /**
     * Lookup the console connection (the channel)
     * for the given console connection id.
     * 
     * @param connectionId the console connection id
     * @return the channel
     */
    /* default */ static Optional<ConsoleConnection>
            lookup(String connectionId) {
        cleanUnused();
        return Optional.ofNullable(connections.get(connectionId))
            .flatMap(ref -> Optional.ofNullable(ref.get()));
    }

    /**
     * Return all connections that belong to the given console as a new
     * unmodifiable set.
     *
     * @param console the console
     * @return the sets the
     */
    public static Set<ConsoleConnection> byConsole(WebConsole console) {
        cleanUnused();
        Set<ConsoleConnection> result = new HashSet<>();
        for (WeakReference<ConsoleConnection> psr : connections.values()) {
            ConsoleConnection psess = psr.get();
            if (psess != null && psess.console != null
                && psess.console.equals(console)) {
                result.add(psess);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Lookup (and create if not found) the console connection
     * for the given console connection id.
     *
     * @param connectionId the connection id
     * @param console the console that this connection belongs to
     * class' constructor if a new channel is created, usually 
     * the console
     * @param supportedLocales the locales supported by the console
     * @param timeout the console connection timeout in milli seconds
     * @return the channel
     */
    /* default */ static ConsoleConnection lookupOrCreate(
            String connectionId, WebConsole console,
            Set<Locale> supportedLocales, long timeout) {
        cleanUnused();
        return connections.computeIfAbsent(connectionId,
            psi -> new ConnectionReference(new ConsoleConnection(
                console, supportedLocales, connectionId, timeout)))
            .get();
    }

    /**
     * Replace the id of the console connection with the new id.
     *
     * @param newConnectionId the new console connection id
     * @return the console connection
     */
    /* default */ ConsoleConnection replaceId(String newConnectionId) {
        connections.remove(connectionId);
        connectionId = newConnectionId;
        connections.put(connectionId, new ConnectionReference(this));
        connected = true;
        return this;
    }

    private ConsoleConnection(WebConsole console, Set<Locale> supportedLocales,
            String connectionId, long timeout) {
        super(console.channel(), console.newEventPipeline());
        this.console = console;
        this.supportedLocales = supportedLocales;
        this.connectionId = connectionId;
        this.timeout = timeout;
        timeoutTimer = Components.schedule(
            tmr -> discard(), Duration.ofMillis(timeout));
    }

    /**
     * Changes the timeout for this {@link ConsoleConnection} to the
     * given value.
     * 
     * @param timeout the timeout in milli seconds
     * @return the console connection for easy chaining
     */
    public ConsoleConnection setTimeout(long timeout) {
        this.timeout = timeout;
        timeoutTimer.reschedule(Duration.ofMillis(timeout));
        return this;
    }

    /**
     * Returns the time when this connection will expire.
     *
     * @return the instant
     */
    public Instant expiresAt() {
        return timeoutTimer.scheduledFor();
    }

    /**
     * Resets the {@link ConsoleConnection}'s timeout.
     */
    public void refresh() {
        timeoutTimer.reschedule(Duration.ofMillis(timeout));
    }

    /**
     * Discards this connection.
     */
    public void discard() {
        connections.remove(connectionId);
        connected = false;
        active = false;
        console.newEventPipeline().fire(new Closed(), this);
    }

    /**
     * Checks if the console connection has become stale (inactive).
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
     * be invoked by the creator of the {@link ConsoleConnection}, by default
     * the {@link ConsoleWeblet}.
     * 
     * @param upstreamChannel the upstream channel (WebSocket connection)
     * @return the console connection for easy chaining
     */
    public ConsoleConnection setUpstreamChannel(IOSubchannel upstreamChannel) {
        if (upstreamChannel == null) {
            throw new IllegalArgumentException();
        }
        this.upstreamChannel = upstreamChannel;
        return this;
    }

    /**
     * Sets or updates associated browser session. This method should only
     * be invoked by the creator of the {@link ConsoleConnection}, by default
     * the {@link ConsoleWeblet}.
     * 
     * @param sessionSupplier the browser session supplier
     * @return the console connection for easy chaining
     */
    public ConsoleConnection
            setSessionSupplier(Supplier<Optional<Session>> sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
        if (locale == null) {
            locale = session().locale();
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
     * The console connection id is used in the communication between the
     * browser and the server. It is not guaranteed to remain the same
     * over time, even if the console connection is maintained. To prevent
     * wrong usage, its visibility is therefore set to package. 
     * 
     * @return the connectionId
     */
    /* default */ String consoleConnectionId() {
        return connectionId;
    }

    /**
     * @return the browser session
     */
    public Session session() {
        return sessionSupplier.get().get();
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
     * Return the console connection's locale. The locale is initialized
     * from the browser session's locale.
     * 
     * @return the locale
     */
    public Locale locale() {
        return locale == null ? Locale.getDefault() : locale;
    }

    /**
     * Sets the locale for this console connection.
     *
     * @param locale the locale
     * @return the console connection
     */
    public ConsoleConnection setLocale(Locale locale) {
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
