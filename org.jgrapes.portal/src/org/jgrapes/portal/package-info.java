/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Provides the components for building a portal based on the
 * core, io and http packages. 
 * 
 * [TOC formatted]
 *
 * Portal Components
 * -----------------
 *
 * ### Portal and PortalWeblet 
 * 
 * The {@link org.jgrapes.portal.Portal} component 
 * is conceptually the main component of the portal. It exchanges events 
 * with the portlets and helper components, using a channel that is 
 * independent of the channel used for the communication with the browser.
 *
 * When created, a {@link org.jgrapes.portal.Portal} component automatically 
 * instantiates a child component of type 
 * {@link org.jgrapes.portal.PortalWeblet} which handles the 
 * communication with the {@link org.jgrapes.http.HttpServer} and thus
 * with the browser. You can think of the 
 * {@link org.jgrapes.portal.PortalWeblet}/{@link org.jgrapes.portal.Portal}
 * pair as a gateway that translates the Input/Output related events on the 
 * HTTP/WebSocket side to portal/portlet related events on the portlet side and 
 * vice versa.
 * 
 * ![Portal Structure](PortalStructure.svg)
 * 
 * In the browser, the portal is implemented as a single page 
 * application. The {@link org.jgrapes.portal.PortalWeblet} provides
 * an initial HTML document that implements the basic structure of
 * the portal. Aside from additional HTTP requests for static resources
 * like JavaScript libraries, CSS, images etc. all information is
 * then exchanged using JSON messages exchanged over a web socket 
 * connection that is established immediately after the initial 
 * HTML has been loaded.
 * 
 * ### Page Resource Providers
 * 
 * The initial HTML document already includes some JavaScript resources
 * like [jQuery](http://jquery.com/) and [jQuery-UI](http://jqueryui.com/).
 * The portlets may, however, require additional libraries in order to
 * work. While it is possible for the portlets to add libraries, it is
 * usually preferable to add such libraries independent from individual
 * portlets in order to avoid duplicate loading and version conflicts.
 * This is done by {@link org.jgrapes.portal.PageResourceProvider}s
 * that fire the required events on portal startup (see below).
 * 
 * ### Portlets
 * 
 * Portlet components represent available portlet types. If a 
 * portlet is actually used (instantiated) in the portal, and state
 * is associated with this instance or instances have to be tracked,
 * the portlet has to create and maintain a server side representation
 * of the instance. How this is done is completely up to the portlet. 
 * A common approach, which is supported by the portlet base class 
 * {@link org.jgrapes.portal.AbstractPortlet}, is shown in the 
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
 * ### Portal Policies
 * 
 * Portal policy components are responsible for establishing the initial
 * set of portlets shown after the portal page has loaded. Usually,
 * there will be a portal policy component that restores the layout from the
 * previous session. {@link org.jgrapes.portal.KVStoreBasedPortalPolicy}
 * is an example of such a component.
 * 
 * There can be more than one portal policy component. A common use case
 * is to have one policy component that maintains the portal layout
 * and another component that ensures that the portal is not empty when
 * a new session is initially created. The demo includes such a component.
 * 
 * Portal Session Startup
 * ----------------------
 * 
 * ### Portal Page Loading
 * 
 * The following diagram shows the start of a portal session 
 * up to the exchange of the first messages on the web socket connection.
 * 
 * ![Boot Event Sequence](PortalBootSeq.svg)
 * 
 * After the portal page has loaded and the web socket connection has been
 * established, all information is exchanged using 
 * [JSON RPC notifications](http://www.jsonrpc.org/specification#notification). 
 * The {@link org.jgrapes.portal.PortalWeblet} processes 
 * {@link org.jgrapes.io.events.Input} events with serialized JSON RPC 
 * data from the web socket channel until the complete JSON RPC notification 
 * has been received. The notification
 * (a {@link org.jgrapes.portal.events.JsonInput} from the servers point 
 * of view) is then fired on the associated
 * {@link org.jgrapes.portal.PortalSession} channel, which allows it to 
 * be intercepted by additional components. Usually, however, it is 
 * handled by the {@link org.jgrapes.portal.Portal} that converts it 
 * to a higher level event that is again fired on the
 * {@link org.jgrapes.portal.PortalSession} channel.
 * 
 * Components such as portlets or portal policies respond by sending 
 * higher level events on the {@link org.jgrapes.portal.PortalSession} 
 * channel. These events are handled by the portal which converts them 
 * to {@link org.jgrapes.portal.events.JsonOutput} events. These are
 * processed by the {@link org.jgrapes.portal.PortalWeblet} which
 * serializes the data and sends it to the websocket using 
 * {@link org.jgrapes.io.events.Output} events.
 * 
 * ### Portal Session Preparation and Configuration
 * 
 * The diagram below shows the complete mandatory sequence of events 
 * following the portal ready message. The diagram uses a 
 * simplified version of the sequence diagram that combines the 
 * {@link org.jgrapes.portal.PortalWeblet} and the 
 * {@link org.jgrapes.portal.Portal} into a single object and leaves out the
 * details about the JSON serialization/deserialization.
 * 
 * ![Portal Ready Event Sequence](PortalReadySeq.svg)
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
 * {@link org.jgrapes.portal.events.AddPageResources} events fired 
 * by the {@link org.jgrapes.portal.PageResourceProvider} components
 * in response to the {@link org.jgrapes.portal.events.PortalReady} event.
 * These cause the portal page to load additional, global resources.
 * 
 * In parallel (also in response to the 
 * {@link org.jgrapes.portal.events.PortalReady} event), each portlet 
 * component fires an {@link org.jgrapes.portal.events.AddPortletType} event.
 * This cause the portal page in the browser to register the portlet type 
 * in the portal's menu of instantiable portlets and to load any additionally 
 * required resources.
 * 
 * When all previously mentioned events have
 * been processed, the portal is considered prepared for usage and a
 * {@link org.jgrapes.portal.events.PortalPrepared} event is generated
 * (by the framework as {@link org.jgrapes.core.CompletionEvent} of the
 * {@link org.jgrapes.portal.events.PortalReady} event). This causes
 * the portal policy to send the last known layout to the portal page
 * in the browser and to send 
 * {@link org.jgrapes.portal.events.RenderPortletRequest} events 
 * for all portlets (portlet instances) in that last known layout.
 * These are the same events as those sent by the browser
 * when the user adds a new portlet instance to the portal page.
 * The portal policy thus "replays" the creation of the portlets.
 * 
 * As completion event of the {@link org.jgrapes.portal.events.PortalPrepared}
 * event, the framework generates a 
 * {@link org.jgrapes.portal.events.PortalConfigured} event which is sent to
 * the portal, indicating that it is now ready for use.
 * 
 * Portal Session Use
 * ------------------
 * 
 * After the portal session has been configured, the system usually
 * waits for input from the user. Changes of the layout of the
 * portal page result in events such as 
 * {@link org.jgrapes.portal.events.AddPortletRequest},
 * {@link org.jgrapes.portal.events.DeletePortletRequest} and
 * {@link org.jgrapes.portal.events.RenderPortletRequest}.
 * 
 * Actions on portlets trigger JSON messages that result in
 * {@link org.jgrapes.portal.events.NotifyPortletModel} events
 * that are processed by the respective portlet component. If,
 * due to the results of the action, the representation of the
 * portlet on the portal page must be updated, the portlet 
 * component fires a 
 * {@link org.jgrapes.portal.events.NotifyPortlet} event.
 * 
 * {@link org.jgrapes.portal.events.NotifyPortlet} events
 * can also be sent unsolicitedly by portlet components if
 * the model data changes independent of user actions.
 * 
 * Writing a Portlet
 * -----------------
 * 
 * Portlets are components that consume and produce events. They
 * don't have to implement a specific interface. Rather they have
 * exhibit a specific behaviour that can be derived from the
 * descritions above. The documentation of the base class
 * {@link org.jgrapes.portal.AbstractPortlet} summarizes
 * the responsibilities of a portal component.
 * 
 * @startuml PortalStructure.svg
 * skinparam packageStyle rectangle
 * allow_mixing
 * 
 * component Browser
 * 
 * package "Conceptual Portal\n(Portal Gateway)" {
 *    class Portal
 * 	  class PortalWeblet
 * 
 *    Portal "1" -left- "1" PortalWeblet
 * }
 * 
 * PortalWeblet "*" -left- "*" Browser
 * 
 * together {
 * 
 *   Portal "1" -right- "1" PortletB
 *   Portal "1" -right- "1" PortletA
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
 * Portal "1" -down- "*" PageResourceProvider
 * Portal "1" -down- "*" PortalPolicy
 * 
 * @enduml
 * 
 * @startuml PortalBootSeq.svg
 * hide footbox
 * 
 * Browser -> PortalWeblet: "GET <portal URL>"
 * activate PortalWeblet
 * PortalWeblet -> Browser: "HTML document"
 * deactivate PortalWeblet
 * activate Browser
 * Browser -> PortalWeblet: "GET <resource1 URL>"
 * activate PortalWeblet
 * PortalWeblet -> Browser: Resource
 * deactivate PortalWeblet
 * Browser -> PortalWeblet: "GET <resource2 URL>"
 * activate PortalWeblet
 * PortalWeblet -> Browser: Resource
 * deactivate PortalWeblet
 * Browser -> PortalWeblet: "GET <Upgrade to WebSocket>"
 * activate PortalWeblet
 * loop while request data
 *     Browser -> PortalWeblet: Input (JSON RPC)
 * end
 * deactivate Browser
 * PortalWeblet -> Portal: JsonInput("portalReady")
 * deactivate PortalWeblet
 * activate Portal
 * Portal -> PortalPolicy: PortalReady
 * deactivate Portal
 * activate PortalPolicy
 * PortalPolicy -> Portal: LastPortalLayout
 * deactivate PortalPolicy
 * activate Portal
 * Portal -> PortalWeblet: JsonOutput("lastPortalLayout")
 * deactivate Portal
 * activate PortalWeblet
 * loop while request data
 *     PortalWeblet -> Browser: Output (JSON RPC)
 * end
 * deactivate PortalWeblet
 * @enduml
 * 
 * @startuml PortalReadySeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "portalReady"
 * activate Portal
 * 
 * par
 * 
 *     loop for all page resource providers
 *         Portal -> PageResourceProviderX: PortalReady
 *         activate PageResourceProviderX
 *         PageResourceProviderX -> Portal: AddPageResources
 *         deactivate PageResourceProviderX
 *         activate Portal
 *         Portal -> Browser: "addPageResources"
 *         deactivate Portal
 *     end
 * 
 *     loop for all portlets
 *         Portal -> PortletX: PortalReady
 *         activate PortletX
 *         PortletX -> Portal: AddPortletType 
 *         deactivate PortletX
 *         activate Portal
 *         Portal -> Browser: "addPortletType"
 *         deactivate Portal
 *     end
 * 
 * end
 * 
 * actor Framework
 * Framework -> PortalPolicy: PortalPrepared
 * deactivate Portal
 * activate PortalPolicy
 * PortalPolicy -> Portal: LastPortalLayout
 * Portal -> Browser: "lastPortalLayout"
 * loop for all portlets to be displayed
 *     PortalPolicy -> PortletX: RenderPortletRequest
 *     activate PortletX
 *     PortletX -> Portal: RenderPortlet
 *     deactivate PortletX
 *     activate Portal
 *     Portal -> Browser: "renderPortlet"
 *     deactivate Portal
 * end
 * deactivate PortalPolicy
 * Framework -> Portal: PortalConfigured
 * activate Portal
 * Portal -> Browser: "portalConfigured"
 * deactivate Portal
 * 
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.portal;