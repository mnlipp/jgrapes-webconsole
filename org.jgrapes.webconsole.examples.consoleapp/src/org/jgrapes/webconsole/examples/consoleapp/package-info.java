/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

/**
 * This package provides a very basic, sample web console configuration
 * which can be used for testing and demonstration purposes. It provides
 * the web console with HTTP (port 8888) and HTTPS (port 8443) access.
 * 
 * ## Component tree
 * 
 * At the root of the component tree is the instance of the
 * {@link org.jgrapes.webconsole.examples.consoleapp.ConsoleApp}.
 * 
 * ![Application hierarchy](app-hierarchy.svg)
 * 
 * The child components of the ConsoleApp are the components related
 * to the network connection, a {@link org.jgrapes.net.SocketServer}
 * for the plain text connection and an {@link org.jgrapes.net.SslCodec}
 * with another SocketServer as its child for the encrypted connection. 
 * Another child is the {@link org.jgrapes.http.HttpServer} that
 * handles the I/O events and converts them to HTTP level events.
 * 
 * The HttpServer has as children all components that handle HTTP
 * request (and generate responses). Most notably the
 * {@link org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet} that
 * handles the main page related resource requests and generates the 
 * provider and conlet related events.
 * 
 * Attached to the WebConsole, we finally have the helper components
 * {@link org.jgrapes.webconsole.base.BrowserLocalBackedKVStore}
 * and {@link org.jgrapes.webconsole.base.KVStoreBasedConsolePolicy}
 * and the collector components for page resource providers and conlets.
 * The collectors pick up the page providers and web conlets in the 
 * classpath and attach them to themselves.
 * 
 * ## Channel usage
 * 
 * In order to avoid unnecessary handler invocations, the components
 * use different channels for different event domains, as shown in the 
 * next picture.
 * 
 * ![Channel usage](app-channels.svg)
 * 
 * The only component interested in events from 
 * the {@link org.jgrapes.net.SocketServer} for the secure connection
 * is the {@link org.jgrapes.net.SslCodec} (end the events that the latter
 * generates are only intended for the former). Therefore these two use
 * a channel of their own (the SocketServer instance for simplicity).
 * 
 * Input from the socket server for plain connections and input from
 * the SSlCodec is handled solemnly by the {@link org.jgrapes.http.HttpServer}.
 * So these three use their own channel. Again, this also applies to the
 * reverse direction, i.e. output events generated by the HttpServer are
 * handled by the SocketServer or the SslCodec that has created the
 * subchannel on which the event (as a response) is fired.
 *
 * HTTP application layer events that are generated by the HttpServer
 * are fired on the httpApplication channel which is used by the components
 * interested in theses events.
 * 
 * Finally, we use the {@link org.jgrapes.webconsole.base.WebConsole}
 * component as channel for all events related to the portal application.
 * 
 * @startuml app-hierarchy.svg
 * [ConsoleApp] *-- [SocketServer:8888]
 * note bottom of [SocketServer:8888]
 *   Connects 
 *   to Network
 * end note
 * 
 * [ConsoleApp] *-- [SslCodec]
 * [SocketServer:8888] -[hidden]right- [SslCodec]
 * [SslCodec] *-- [SocketServer:8443]
 * note bottom of [SocketServer:8443]
 *   Connects 
 *   to Network
 * end note
 * 
 * [ConsoleApp] *-- [HttpServer]
 * [SslCodec] -[hidden]right- [HttpServer]
 * [HttpServer] *-up- [PreferencesStore]
 * [HttpServer] *-right- [InMemorySessionManager]
 * [HttpServer] *-up- [LanguageSelector]
 * 
 * package "Conceptual WebConsole" {
 *   [VueJsConsoleWeblet] *-- [WebConsole]
 * }
 * [HttpServer] *-down- [VueJsConsoleWeblet]
 * 
 * [WebConsole] *-right- [BrowserLocalBackedKVStore]
 * [WebConsole] *-- [KVStoreBasedConsolePolicy]
 * [WebConsole] *-- [ComponentCollector\nfor page resources]
 * [WebConsole] *-- [ComponentCollector\nfor conlets]
 * 
 * package "Providers and Conlets" {
 *   [Some component]
 * }
 * 
 * [ComponentCollector\nfor page resources] *-- [Some component]
 * [ComponentCollector\nfor conlets] *-- [Some component]
 * @enduml
 * 
 * @startuml app-channels.svg
 * () "httpTransport" as hT
 * hT .up. [SocketServer:8888]
 * note right of [SocketServer:8888]
 *   Connects 
 *   to Network
 * end note
 * 
 * hT .right. [SslCodec]
 * () "securedChannel" as sC
 * sC .left. [SslCodec]
 * sC -right- [SocketServer:8443]
 * note right of [SocketServer:8443]
 *   Connects 
 *   to Network
 * end note
 * 
 * hT .down. [HttpServer]
 * 
 * () "httpApplication" as http
 * http .up. [HttpServer]
 * 
 * [PreferencesStore] .right. http
 * [InMemorySessionManager] .up. http
 * [LanguageSelector] .up. http
 * 
 * package "Conceptual WebConsole" {
 *   [VueJsConsoleWeblet] .left. http
 *   [VueJsConsoleWeblet] *-down- [WebConsole]
 * }
 * 
 * () "console" as con
 * [WebConsole] -right- con
 * con .. [BrowserLocalBackedKVStore]
 * con .. [KVStoreBasedConsolePolicy]
 * 
 * package "Providers and Conlets" {
 *   con .up. [Some component]
 * }
 * @enduml
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.jgrapes.webconsole.examples.consoleapp;
