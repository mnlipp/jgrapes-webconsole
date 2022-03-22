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

/**
 * Provides the components for building web consoles based on the
 * core, io and http packages. 
 * 
 * [TOC formatted]
 * 
 * Overview
 * --------
 * 
 * A web console built with the components provided here is a single
 * page application (SPA) that &mdash;from the user's point of view&mdash;
 * consists of a fixed frame with configurable content. The frame provides
 * some means to add content (typically by using a dropdown menu) and to 
 * configure global settings such as the locale.
 * 
 * The content of the frame is provided by web console display components 
 * or "conlets" for short. These components typically provide a summary
 * or preview display that can be put on an overview panel in a dashboard
 * style and a large view that is supposed to fill the complete frame.
 * 
 * Tabs or a menu in a side bar can be used to switch between
 * the overview panel(s) and the large views of the different conlets. 
 *
 * Web Console Components
 * ----------------------
 *
 * ### WebConsole and ConsoleWeblet 
 * 
 * The {@link org.jgrapes.webconsole.base.WebConsole} component 
 * is conceptually the main component of a web console. It exchanges events 
 * with the display components and helper components, 
 * using a channel that is independent of the channel used for the 
 * communication with the browser.
 *
 * The {@link org.jgrapes.webconsole.base.WebConsole} component is 
 * automatically instantiated as a child component of a 
 * {@link org.jgrapes.webconsole.base.ConsoleWeblet} which handles the 
 * communication with the {@link org.jgrapes.http.HttpServer} and thus
 * with the browser. You can think of the 
 * {@link org.jgrapes.webconsole.base.ConsoleWeblet}/{@link org.jgrapes.webconsole.base.WebConsole}
 * pair as a gateway that translates the Input/Output related events on the 
 * HTTP/WebSocket side to web console related events and vice versa.
 * 
 * ![Web Console Structure](ConsoleStructure.svg)
 * 
 * In the browser, the web console is implemented as a single page 
 * application (SPA). The {@link org.jgrapes.webconsole.base.ConsoleWeblet} 
 * provides the initial HTML document that implements the basic structure of
 * the web console. Aside from additional HTTP requests for static resources
 * like JavaScript libraries, CSS, images etc. all information is
 * then exchanged using JSON messages sent over a web socket 
 * connection that is established immediately after the initial 
 * HTML has been loaded. The information exchanged includes, 
 * in particular, the registration of web console display components 
 * and helper components, which are described below.
 * 
 * {@link org.jgrapes.webconsole.base.ConsoleWeblet} is an abstract
 * class that has to be extended by derived classes such as
 * {@link org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet}.
 * The derived classes provide the resources for an actual console 
 * page. This allows differently styles consoles to be build on a
 * common foundation. The {@link org.jgrapes.webconsole.base.ConsoleWeblet}
 * provides some JavaScript classes
 * <a href="jsdoc/classes/Console.html">functions</a>
 * that can be used to build the SPA.
 * 
 * ### Page Resource Providers
 * 
 * The initial HTML document may already include some JavaScript 
 * resources which are required to implement the basic functions 
 * (such as providing the header with the web console related menus).
 * The web console display components may, however, require additional libraries 
 * in order to work. While it is possible for the web console display components 
 * to add libraries, it is usually preferable to add such libraries 
 * independent from individual web console display components in order to avoid 
 * duplicate loading and version conflicts.
 * This is done by {@link org.jgrapes.webconsole.base.PageResourceProvider}s
 * that fire the required events on web console startup (see below).
 * 
 * ### Web Console Display Components
 * 
 * Web console display components ("conlet components") represent available 
 * content types. When a display component is actually used 
 * (instantiated) in the web console, state information that represents
 * the instance has to be created and maintained on the server side. 
 * How this is done is completely up to the web console display component. 
 * A common approach, which is supported by the display component base class 
 * {@link org.jgrapes.webconsole.base.AbstractConlet}, is shown in the 
 * diagram above. 
 * 
 * Using this approach, the display component creates a data object 
 * for each instance as an item stored in the browser session. This
 * couples the lifetime of the data instances with the 
 * lifetime of the general session data, which is what you'd 
 * usually expect. Note that the data object is conceptually 
 * a view of some model maintained elsewhere. If the state information
 * associated with this view (e.g. columns displayed or hidden in
 * a table representation) needs to be persisted across 
 * sessions, it's up to the web console component to do this, using 
 * a persistence mechanism of its choice.
 * 
 * The functionality that must be provided by a web console display 
 * component with respect to its presentation on the web console page 
 * will be discussed later, after all component types and their interactions 
 * have been introduced.
 * 
 * ### Web Console Policies
 * 
 * Web console policy components are responsible for establishing the initial
 * set of web console display components shown after the web console page has 
 * loaded. Usually, there will be a web console policy component that 
 * restores the layout from the previous session. 
 * {@link org.jgrapes.webconsole.base.KVStoreBasedConsolePolicy}
 * is an example of such a component.
 * 
 * There can be more than one web console policy component. A common use case
 * is to have one policy component that maintains the web console layout
 * across reloads and another component that ensures that the web console 
 * is not empty when a new session is initially created. The demo 
 * includes such a component.
 * 
 * Web Console Session Startup
 * ---------------------------
 * 
 * ### Web Console Page Loading
 * 
 * The following diagram shows the start of a web console session 
 * up to the exchange of the first messages on the web socket connection.
 * 
 * ![Boot Event Sequence](ConsoleBootSeq.svg)
 * 
 * After the web console page has loaded and the web socket connection has been
 * established, all information is exchanged using 
 * [JSON RPC notifications](http://www.jsonrpc.org/specification#notification). 
 * The {@link org.jgrapes.webconsole.base.ConsoleWeblet} processes 
 * {@link org.jgrapes.io.events.Input} events with serialized JSON RPC 
 * data from the web socket channel until the complete JSON RPC notification 
 * has been received. The notification
 * (a {@link org.jgrapes.webconsole.base.events.JsonInput} from the servers point 
 * of view) is then fired on the associated
 * {@link org.jgrapes.webconsole.base.ConsoleSession} channel, which allows it to 
 * be intercepted by additional components. Usually, however, it is 
 * handled by the {@link org.jgrapes.webconsole.base.WebConsole} that converts it 
 * to a higher level event that is again fired on the
 * {@link org.jgrapes.webconsole.base.ConsoleSession} channel.
 * 
 * Components such as web console components or web console policies respond 
 * by sending 
 * {@link org.jgrapes.webconsole.base.events.ConsoleCommand}s on the
 * {@link org.jgrapes.webconsole.base.ConsoleSession} channel as responses.
 * The {@link org.jgrapes.webconsole.base.events.ConsoleCommand}s are handled 
 * by the {@link org.jgrapes.webconsole.base.ConsoleWeblet} which serializes 
 * the data and sends it to the websocket using 
 * {@link org.jgrapes.io.events.Output} events.
 * 
 * ### Web Console Session Preparation and Configuration
 * 
 * The diagram below shows the complete mandatory sequence of events 
 * following the web console ready message. The diagram uses a 
 * simplified version of the sequence diagram that combines the 
 * {@link org.jgrapes.webconsole.base.ConsoleWeblet} and the 
 * {@link org.jgrapes.webconsole.base.WebConsole} into a single object and leaves out the
 * details about the JSON serialization/deserialization.
 * 
 * ![WebConsole Ready Event Sequence](ConsoleReadySeq.svg)
 * 
 * The remaining part of this section provides an overview of the 
 * preparation and configuration sequence. Detailed information 
 * about the purpose and handling of the different events can be 
 * found in their respective JavaDoc or in the documentation of 
 * the components that handle them. Note that the these 
 * detailed descriptions use the simplified version of the 
 * sequence diagram as well.
 * 
 * The preparation sequence starts with 
 * {@link org.jgrapes.webconsole.base.events.AddPageResources} events fired 
 * by the {@link org.jgrapes.webconsole.base.PageResourceProvider} components
 * in response to the {@link org.jgrapes.webconsole.base.events.ConsoleReady} event.
 * These cause the web console page to load additional, global resources.
 * 
 * In parallel (also in response to the 
 * {@link org.jgrapes.webconsole.base.events.ConsoleReady} event), each 
 * display component 
 * fires an {@link org.jgrapes.webconsole.base.events.AddConletType} event.
 * This causes the web console page in the browser to register the 
 * web console component type in the web console's menu of 
 * instantiable display components and to load any 
 * additionally required resources.
 * 
 * When all previously mentioned events have
 * been processed, the web console is considered prepared for usage and a
 * {@link org.jgrapes.webconsole.base.events.ConsolePrepared} event is generated
 * (by the framework as {@link org.jgrapes.core.CompletionEvent} of the
 * {@link org.jgrapes.webconsole.base.events.ConsoleReady} event). This causes
 * the web console policy to send the last known layout to the web console page
 * in the browser and to send 
 * {@link org.jgrapes.webconsole.base.events.RenderConletRequest} events 
 * for all display components (instances, to be precise) in 
 * that last known layout. These are the same events as those sent by the 
 * browser when the user adds a new web console component instance to the web 
 * console page. The web console policy thus "replays" the creation of the 
 * web console components and the portal page uses the last layout 
 * information to restore the previous positions.
 * 
 * As completion event of the {@link org.jgrapes.webconsole.base.events.ConsolePrepared}
 * event, the framework generates a 
 * {@link org.jgrapes.webconsole.base.events.ConsoleConfigured} event which is sent to
 * the web console, indicating that it is now ready for use.
 * 
 * Web Console Session Use
 * -----------------------
 * 
 * After the web console session has been configured, the system usually
 * waits for input from the user. Changes of the layout of the
 * web console page result in events such as 
 * {@link org.jgrapes.webconsole.base.events.AddConletRequest},
 * {@link org.jgrapes.webconsole.base.events.RenderConletRequest} and
 * {@link org.jgrapes.webconsole.base.events.ConletDeleted}.
 * 
 * Actions on web console components trigger JSON messages that result in
 * {@link org.jgrapes.webconsole.base.events.NotifyConletModel} events
 * which are processed by the respective display component. If,
 * due to the results of the action, the representation of the
 * display component on the web console page must be updated, the 
 * web console component  fires a 
 * {@link org.jgrapes.webconsole.base.events.NotifyConletView} event.
 * 
 * {@link org.jgrapes.webconsole.base.events.NotifyConletView} events
 * can also be sent unsolicitedly by web console components if
 * the model data changes independent of user actions.
 * 
 * Writing a web console display component (Conlet)
 * ------------------------------------------------
 * 
 * Web console display components ("conlets") are components that consume and 
 * produce events. They
 * don't have to implement a specific interface. Rather, they have
 * to exhibit a specific behavior that can be derived from the
 * descriptions above. The documentation of the base class
 * {@link org.jgrapes.webconsole.base.AbstractConlet} summarizes
 * the responsibilities of a web console component.
 * 
 * Display components consist of (at least one) Java class and HTML 
 * generated by this class. Optionally, a display component can 
 * contribute style information and JavaScript 
 * (see {@link org.jgrapes.webconsole.base.events.AddConletType}).
 * It may (and should) make use of the styles and 
 * <a href="jsdoc/index.html">classes</a> 
 * provided by the web console JavaScript object in the browser page.
 * 
 * @see "[Client side JavaScript classes](jsdoc/index.html)"
 * 
 * @startuml ConsoleStructure.svg
 * skinparam packageStyle rectangle
 * allow_mixing
 * 
 * component Browser
 * 
 * package "Conceptual WebConsole\n(WebConsole Gateway)" {
 *    class WebConsole
 * 	  class ConsoleWeblet
 * 
 *    WebConsole "1" -left- "1" ConsoleWeblet
 * }
 * 
 * ConsoleWeblet "*" -left- "*" Browser
 * 
 * together {
 * 
 *   WebConsole "1" -right- "1" ConletB
 *   WebConsole "1" -right- "1" ConletA
 * 
 *   class ConletAData {
 *     -conletId: String
 *   }
 * 
 *   ConletAData "*" -up- "1" ConletA
 * 
 *   class ConletBData {
 *     -conletId: String
 *   }
 * 
 *   ConletBData "*" -up- "1" ConletB
 *   
 *   ConletAData "*" -up-* "1" Session
 *   ConletBData "*" -up-* "1" Session
 * }
 * 
 * WebConsole "1" -down- "*" PageResourceProvider
 * WebConsole "1" -down- "*" ConsolePolicy
 * 
 * @enduml
 * 
 * @startuml ConsoleBootSeq.svg
 * hide footbox
 * 
 * Browser -> ConsoleWeblet: "GET <console URL>"
 * activate ConsoleWeblet
 * ConsoleWeblet -> Browser: "HTML document"
 * deactivate ConsoleWeblet
 * activate Browser
 * Browser -> ConsoleWeblet: "GET <resource1 URL>"
 * activate ConsoleWeblet
 * ConsoleWeblet -> Browser: Resource
 * deactivate ConsoleWeblet
 * Browser -> ConsoleWeblet: "GET <resource2 URL>"
 * activate ConsoleWeblet
 * ConsoleWeblet -> Browser: Resource
 * deactivate ConsoleWeblet
 * Browser -> ConsoleWeblet: "GET <Upgrade to WebSocket>"
 * activate ConsoleWeblet
 * loop while request data
 *     Browser -> ConsoleWeblet: Input (JSON RPC)
 * end
 * deactivate Browser
 * ConsoleWeblet -> WebConsole: JsonInput("consoleReady")
 * deactivate ConsoleWeblet
 * activate WebConsole
 * WebConsole -> ConsolePolicy: ConsoleReady
 * deactivate WebConsole
 * activate ConsolePolicy
 * ConsolePolicy -> ConsoleWeblet: LastConsoleLayout
 * deactivate ConsolePolicy
 * activate ConsoleWeblet
 * loop while request data
 *     ConsoleWeblet -> Browser: Output (JSON RPC)
 * end
 * deactivate ConsoleWeblet
 * @enduml
 * 
 * @startuml ConsoleReadySeq.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "consoleReady"
 * activate WebConsole
 * 
 * par
 * 
 *     loop for all page resource providers
 *         WebConsole -> PageResourceProviderX: ConsoleReady
 *         activate PageResourceProviderX
 *         PageResourceProviderX -> WebConsole: AddPageResources
 *         deactivate PageResourceProviderX
 *         activate WebConsole
 *         WebConsole -> Browser: "addPageResources"
 *         deactivate WebConsole
 *     end
 * 
 *     loop for all conlets
 *         WebConsole -> ConletX: ConsoleReady
 *         activate ConletX
 *         ConletX -> WebConsole: AddConletType 
 *         deactivate ConletX
 *         activate WebConsole
 *         WebConsole -> Browser: "addConletType"
 *         deactivate WebConsole
 *     end
 * 
 * end
 * 
 * actor Framework
 * Framework -> ConsolePolicy: ConsolePrepared
 * deactivate WebConsole
 * activate ConsolePolicy
 * ConsolePolicy -> WebConsole: LastConsoleLayout
 * WebConsole -> Browser: "lastConsoleLayout"
 * loop for all conlets to be displayed
 *     ConsolePolicy -> ConletX: RenderConletRequest
 *     activate ConletX
 *     ConletX -> WebConsole: RenderConlet
 *     deactivate ConletX
 *     activate WebConsole
 *     WebConsole -> Browser: "renderConlet"
 *     deactivate WebConsole
 * end
 * deactivate ConsolePolicy
 * Framework -> WebConsole: ConsoleConfigured
 * activate WebConsole
 * WebConsole -> Browser: "consoleConfigured"
 * deactivate WebConsole
 * 
 * @enduml
 * 
 * @startuml package-hierarchy.svg
 * skinparam svgLinkTarget _parent
 * 
 * package org.jgrapes {
 *     package "JGrapes Core Components" {
 *     }
 * 
 *     package org.jgrapes.webconsole.base \
 *      [[../../webconsole/base/package-summary.html#package.description]] {
 *     }
 * 
 *     package org.jgrapes.webconsole.vuejs \
 *      [[../../webconsole/vuejs/package-summary.html#package.description]] {
 *     }
 * 
 *     package org.jgrapes.webconsole.bootstrap4 \
 *      [[../../webconsole/bootstrap4/package-summary.html#package.description]] {
 *     }
 * 
 *     package org.jgrapes.webconsole.jqueryui \
 *      [[../../webconsole/jqueryui/package-summary.html#package.description]] {
 *     }
 * 
 *     package "org.jgrapes.webconsole.jqueryui.themes.*" {
 *     }
 * }
 * 
 * "JGrapes Core Components" <.. org.jgrapes.webconsole.base
 * org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.jqueryui"
 * org.jgrapes.webconsole.jqueryui <.. "org.jgrapes.webconsole.jqueryui.themes.*"
 * org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.bootstrap4"
 * org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.vuejs"
 * 
 * @enduml
 */
package org.jgrapes.webconsole.base;