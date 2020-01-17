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
 * Provides the components for building a portal based on the
 * core, io and http packages. 
 * 
 * [TOC formatted]
 *
 * WebConsole Components
 * -----------------
 *
 * ### WebConsole and ConsoleWeblet 
 * 
 * The {@link org.jgrapes.webcon.base.WebConsole} component 
 * is conceptually the main component of the portal. It exchanges events 
 * with the portlets and helper components, using a channel that is 
 * independent of the channel used for the communication with the browser.
 *
 * The {@link org.jgrapes.webcon.base.WebConsole} component is automatically
 * instantiated as a child component of a 
 * {@link org.jgrapes.webcon.base.ConsoleWeblet} which handles the 
 * communication with the {@link org.jgrapes.http.HttpServer} and thus
 * with the browser. You can think of the 
 * {@link org.jgrapes.webcon.base.ConsoleWeblet}/{@link org.jgrapes.webcon.base.WebConsole}
 * pair as a gateway that translates the Input/Output related events on the 
 * HTTP/WebSocket side to portal/portlet related events on the portlet side and 
 * vice versa.
 * 
 * ![WebConsole Structure](PortalStructure.svg)
 * 
 * In the browser, the portal is implemented as a single page 
 * application. The {@link org.jgrapes.webcon.base.ConsoleWeblet} provides
 * an initial HTML document that implements the basic structure of
 * the portal. Aside from additional HTTP requests for static resources
 * like JavaScript libraries, CSS, images etc. all information is
 * then exchanged using JSON messages exchanged over a web socket 
 * connection that is established immediately after the initial 
 * HTML has been loaded.
 * 
 * ### Page Resource Providers
 * 
 * The initial HTML document already includes some basic JavaScript 
 * resources which are required to implement the basic functions 
 * (such as [jQuery](http://jquery.com/)).
 * The portlets may, however, require additional libraries in order to
 * work. While it is possible for the portlets to add libraries, it is
 * usually preferable to add such libraries independent from individual
 * portlets in order to avoid duplicate loading and version conflicts.
 * This is done by {@link org.jgrapes.webcon.base.PageResourceProvider}s
 * that fire the required events on portal startup (see below).
 * 
 * ### Portlets
 * 
 * ConsoleComponent components represent available portlet types. If a 
 * portlet is actually used (instantiated) in the portal, and state
 * is associated with this instance or instances have to be tracked,
 * the portlet has to create and maintain a server side representation
 * of the instance. How this is done is completely up to the portlet. 
 * A common approach, which is supported by the portlet base class 
 * {@link org.jgrapes.webcon.base.AbstractComponent}, is shown in the 
 * diagram above. 
 * 
 * Using this approach, the portlet creates a portlet data object 
 * for each instance as an item stored in the browser session. This
 * couples the lifetime of the portlet data instances with the 
 * lifetime of the general session data, which is what you'd 
 * usually expect. Note that the portal data object is conceptually 
 * a view of some model maintained elsewhere. If the state information
 * associated with this view (e.g. columns displayed or hidden in
 * a table representation) needs to be persisted across 
 * sessions, it's up to the portlet to do this, using a persistence 
 * mechanism of its choice.
 * 
 * The functionality that must be provided by a portlet with respect 
 * to its display on the portal page will be discussed later, after
 * all components and their interactions have been introduced.
 * 
 * ### WebConsole Policies
 * 
 * WebConsole policy components are responsible for establishing the initial
 * set of portlets shown after the portal page has loaded. Usually,
 * there will be a portal policy component that restores the layout from the
 * previous session. {@link org.jgrapes.webcon.base.KVStoreBasedConsolePolicy}
 * is an example of such a component.
 * 
 * There can be more than one portal policy component. A common use case
 * is to have one policy component that maintains the portal layout
 * and another component that ensures that the portal is not empty when
 * a new session is initially created. The demo includes such a component.
 * 
 * WebConsole Session Startup
 * ----------------------
 * 
 * ### WebConsole Page Loading
 * 
 * The following diagram shows the start of a portal session 
 * up to the exchange of the first messages on the web socket connection.
 * 
 * ![Boot Event Sequence](PortalBootSeq.svg)
 * 
 * After the portal page has loaded and the web socket connection has been
 * established, all information is exchanged using 
 * [JSON RPC notifications](http://www.jsonrpc.org/specification#notification). 
 * The {@link org.jgrapes.webcon.base.ConsoleWeblet} processes 
 * {@link org.jgrapes.io.events.Input} events with serialized JSON RPC 
 * data from the web socket channel until the complete JSON RPC notification 
 * has been received. The notification
 * (a {@link org.jgrapes.webcon.base.events.JsonInput} from the servers point 
 * of view) is then fired on the associated
 * {@link org.jgrapes.webcon.base.ConsoleSession} channel, which allows it to 
 * be intercepted by additional components. Usually, however, it is 
 * handled by the {@link org.jgrapes.webcon.base.WebConsole} that converts it 
 * to a higher level event that is again fired on the
 * {@link org.jgrapes.webcon.base.ConsoleSession} channel.
 * 
 * Components such as portlets or portal policies respond by sending 
 * {@link org.jgrapes.webcon.base.events.ConsoleCommand}s on the
 * {@link org.jgrapes.webcon.base.ConsoleSession} channel as responses.
 * The {@link org.jgrapes.webcon.base.events.ConsoleCommand}s are handled 
 * by the {@link org.jgrapes.webcon.base.ConsoleWeblet} which serializes 
 * the data and sends it to the websocket using 
 * {@link org.jgrapes.io.events.Output} events.
 * 
 * ### WebConsole Session Preparation and Configuration
 * 
 * The diagram below shows the complete mandatory sequence of events 
 * following the portal ready message. The diagram uses a 
 * simplified version of the sequence diagram that combines the 
 * {@link org.jgrapes.webcon.base.ConsoleWeblet} and the 
 * {@link org.jgrapes.webcon.base.WebConsole} into a single object and leaves out the
 * details about the JSON serialization/deserialization.
 * 
 * ![WebConsole Ready Event Sequence](PortalReadySeq.svg)
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
 * {@link org.jgrapes.webcon.base.events.AddPageResources} events fired 
 * by the {@link org.jgrapes.webcon.base.PageResourceProvider} components
 * in response to the {@link org.jgrapes.webcon.base.events.ConsoleReady} event.
 * These cause the portal page to load additional, global resources.
 * 
 * In parallel (also in response to the 
 * {@link org.jgrapes.webcon.base.events.ConsoleReady} event), each portlet 
 * component fires an {@link org.jgrapes.webcon.base.events.AddComponentType} event.
 * This cause the portal page in the browser to register the portlet type 
 * in the portal's menu of instantiable portlets and to load any additionally 
 * required resources.
 * 
 * When all previously mentioned events have
 * been processed, the portal is considered prepared for usage and a
 * {@link org.jgrapes.webcon.base.events.ConsolePrepared} event is generated
 * (by the framework as {@link org.jgrapes.core.CompletionEvent} of the
 * {@link org.jgrapes.webcon.base.events.ConsoleReady} event). This causes
 * the portal policy to send the last known layout to the portal page
 * in the browser and to send 
 * {@link org.jgrapes.webcon.base.events.RenderComponentRequest} events 
 * for all portlets (portlet instances) in that last known layout.
 * These are the same events as those sent by the browser
 * when the user adds a new portlet instance to the portal page.
 * The portal policy thus "replays" the creation of the portlets.
 * 
 * As completion event of the {@link org.jgrapes.webcon.base.events.ConsolePrepared}
 * event, the framework generates a 
 * {@link org.jgrapes.webcon.base.events.ConsoleConfigured} event which is sent to
 * the portal, indicating that it is now ready for use.
 * 
 * WebConsole Session Use
 * ------------------
 * 
 * After the portal session has been configured, the system usually
 * waits for input from the user. Changes of the layout of the
 * portal page result in events such as 
 * {@link org.jgrapes.webcon.base.events.AddComponentRequest},
 * {@link org.jgrapes.webcon.base.events.DeleteComponentRequest} and
 * {@link org.jgrapes.webcon.base.events.RenderComponentRequest}.
 * 
 * Actions on portlets trigger JSON messages that result in
 * {@link org.jgrapes.webcon.base.events.NotifyComponentModel} events
 * that are processed by the respective portlet component. If,
 * due to the results of the action, the representation of the
 * portlet on the portal page must be updated, the portlet 
 * component fires a 
 * {@link org.jgrapes.webcon.base.events.NotifyPortletView} event.
 * 
 * {@link org.jgrapes.webcon.base.events.NotifyPortletView} events
 * can also be sent unsolicitedly by portlet components if
 * the model data changes independent of user actions.
 * 
 * Writing a ConsoleComponent
 * -----------------
 * 
 * Portlets are components that consume and produce events. They
 * don't have to implement a specific interface. Rather they have
 * exhibit a specific behavior that can be derived from the
 * descriptions above. The documentation of the base class
 * {@link org.jgrapes.webcon.base.AbstractComponent} summarizes
 * the responsibilities of a portal component.
 * 
 * Portlets consist of (at least one) Java class and HTML generated
 * by this class. Optionally, a portlet can contribute style information
 * and JavaScript (see {@link org.jgrapes.webcon.base.events.AddComponentType}).
 * It may (and should) make use of the styles and 
 * <a href="jsdoc/module-portal-base-resource_jgportal.html">functions</a> 
 * provided by the portal.
 * 
 * 
 * @startuml PortalStructure.svg
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
 *   WebConsole "1" -right- "1" PortletB
 *   WebConsole "1" -right- "1" PortletA
 * 
 *   class PortletAData {
 *     -portletId: String
 *   }
 * 
 *   PortletAData "*" -up- "1" PortletA
 * 
 *   class PortletBData {
 *     -portletId: String
 *   }
 * 
 *   PortletBData "*" -up- "1" PortletB
 *   
 *   PortletAData "*" -up-* "1" Session
 *   PortletBData "*" -up-* "1" Session
 * }
 * 
 * WebConsole "1" -down- "*" PageResourceProvider
 * WebConsole "1" -down- "*" PortalPolicy
 * 
 * @enduml
 * 
 * @startuml PortalBootSeq.svg
 * hide footbox
 * 
 * Browser -> ConsoleWeblet: "GET <portal URL>"
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
 * ConsoleWeblet -> WebConsole: JsonInput("portalReady")
 * deactivate ConsoleWeblet
 * activate WebConsole
 * WebConsole -> PortalPolicy: ConsoleReady
 * deactivate WebConsole
 * activate PortalPolicy
 * PortalPolicy -> ConsoleWeblet: LastConsoleLayout
 * deactivate PortalPolicy
 * activate ConsoleWeblet
 * loop while request data
 *     ConsoleWeblet -> Browser: Output (JSON RPC)
 * end
 * deactivate ConsoleWeblet
 * @enduml
 * 
 * @startuml PortalReadySeq.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "portalReady"
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
 *     loop for all portlets
 *         WebConsole -> PortletX: ConsoleReady
 *         activate PortletX
 *         PortletX -> WebConsole: AddComponentType 
 *         deactivate PortletX
 *         activate WebConsole
 *         WebConsole -> Browser: "addPortletType"
 *         deactivate WebConsole
 *     end
 * 
 * end
 * 
 * actor Framework
 * Framework -> PortalPolicy: ConsolePrepared
 * deactivate WebConsole
 * activate PortalPolicy
 * PortalPolicy -> WebConsole: LastConsoleLayout
 * WebConsole -> Browser: "lastPortalLayout"
 * loop for all portlets to be displayed
 *     PortalPolicy -> PortletX: RenderComponentRequest
 *     activate PortletX
 *     PortletX -> WebConsole: RenderComponent
 *     deactivate PortletX
 *     activate WebConsole
 *     WebConsole -> Browser: "renderPortlet"
 *     deactivate WebConsole
 * end
 * deactivate PortalPolicy
 * Framework -> WebConsole: ConsoleConfigured
 * activate WebConsole
 * WebConsole -> Browser: "portalConfigured"
 * deactivate WebConsole
 * 
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.webcon.base;