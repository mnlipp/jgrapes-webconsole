/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.portal.events;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Event;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.RenderSupport;
import org.jgrapes.portal.ResourceByUrl;
import org.jgrapes.portal.ResourceNotModified;
import org.jgrapes.portal.ResourceProvided;
import org.jgrapes.portal.ResourceResult;

/**
 * An event that signals the request for a resource by the browser.
 * 
 * This event is effectively a "transformed" {@link GetRequest}. It 
 * simplifies handling of such an event by portal components, because
 * they can simply set a result of type {@link ResourceResult} and
 * thus need no knowledge about generating all the events required to 
 * properly respond to a {@link GetRequest}.
 * 
 * The complete sequence of events is shown in the diagram.
 * 
 * ![Resource Response Sequence](ResourceResponseSeq.svg)
 * 
 * Of course, due to internal buffering, the "Response Header" data
 * and the "Response body" data may collapse in a single message
 * that is sent to the browser (in case of a small resource).
 * 
 * If a value is provided by {@link #ifModifiedSince()},
 * and the resource has not changed since the given instant,
 * a resource provider may set {@link ResourceNotModified} as
 * result. This information will be forwarded to the browser.
 * For a result of type {@link ResourceByUrl}, the check
 * for modification will be made automatically, using information
 * derived from the {@link URL}.
 * 
 * Handlers of {@link ResourceRequest} events use usually only
 * the information provided by {@link #resourceUri()}. The other
 * items are needed by the handler of the {@link ResourceRequestCompleted}
 * event (the portal) to generate the response for the {@link GetRequest}.
 * 
 * If none of the provided {@link ResourceResult} type matches the
 * requirements of the resource provider, it can set
 * {@link ResourceProvided} as result. This signals that it genertes
 * the response itself.
 * 
 * ![Extended Resource Response Sequence](ResourceResponseSelfSeq.svg)
 * 
 * @startuml ResourceResponseSeq.svg
 * hide footbox
 * 
 * activate Portal
 * actor Framework
 * Portal -> ResourceProvider: ResourceRequest
 * activate ResourceProvider
 * deactivate ResourceProvider
 * deactivate Portal
 * Framework -> Portal: ResourceRequestCompleted
 * activate Portal
 * Portal -> Browser: "Response Header"
 * loop until end of data
 *     Portal -> Browser: Output
 *     Portal -> Browser: "Response body"
 *     deactivate Portal
 * end loop
 * deactivate ResourceProvider
 * deactivate Browser
 * @enduml
 * 
 * @startuml ResourceResponseSelfSeq.svg
 * hide footbox
 * 
 * activate Portal
 * actor Framework
 * Portal -> ResourceProvider: ResourceRequest
 * activate ResourceProvider
 * Framework -> Portal: ResourceRequestCompleted
 * deactivate Portal
 * ResourceProvider -> Browser: "Response Header"
 * loop until end of data
 *     ResourceProvider -> Browser: Output
 *     ResourceProvider -> Browser: "Response body"
 *     deactivate Portal
 * end loop
 * deactivate ResourceProvider
 * deactivate Browser
 * 
 * @enduml
 */
@SuppressWarnings("PMD.DataClass")
public class ResourceRequest extends Event<ResourceResult> {
	
	private final URI resourceUri;
	private final Instant ifModifiedSince;
	private final HttpRequest httpRequest;
	private final IOSubchannel httpChannel;
	private final RenderSupport renderSupport;

	/**
	 * Creates a new request, including the associated 
	 * {@link ResourceRequestCompleted} event.
	 * 
	 * @param resourceUri the requested resource
	 * @param httpRequest the original HTTP request
	 * @param httpChannel the channel that the HTTP request was received on
	 * @param renderSupport the render support
	 */
	public ResourceRequest(URI resourceUri, Instant ifModifiedSince,
			HttpRequest httpRequest, IOSubchannel httpChannel, 
			RenderSupport renderSupport) {
		this.resourceUri = resourceUri;
		this.ifModifiedSince = ifModifiedSince;
		this.httpRequest = httpRequest;
		this.httpChannel = httpChannel;
		this.renderSupport = renderSupport;
		new ResourceRequestCompleted(this);
	}


	/**
	 * @return the resourceUri
	 */
	public URI resourceUri() {
		return resourceUri;
	}

	
	/**
	 * If not null, this value may be used to decide if the
	 * resource must be refreshed.
	 *
	 * @return the instant
	 */
	public Optional<Instant> ifModifiedSince() {
		return Optional.ofNullable(ifModifiedSince);
	}


	/**
	 * Returns the "raw" request as provided by the HTTP decoder.
	 * 
	 * @return the request
	 */
	public HttpRequest httpRequest() {
		return httpRequest;
	}

	/**
	 * @return the httpChannel
	 */
	public IOSubchannel httpChannel() {
		return httpChannel;
	}
	
	/**
	 * Returns the render support.
	 * 
	 * @return the render support
	 */
	public RenderSupport renderSupport() {
		return renderSupport;
	}

}
