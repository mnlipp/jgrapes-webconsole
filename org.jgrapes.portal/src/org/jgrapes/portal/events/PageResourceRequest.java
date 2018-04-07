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

package org.jgrapes.portal.events;

import java.net.URI;
import java.time.Instant;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.RenderSupport;

/**
 * An event that signals the request of a resource that is to be added
 * to the `<HEAD>` section of the page.
 *
 * Requests for such resources are usually
 * generated during portal boot. See the description of
 * {@link AddPageResources} for details.
 */
public class PageResourceRequest extends ResourceRequest {

    /**
     * Creates a new request.
     * 
     * @param resourceUri the requested resource
     * @param httpRequest the original HTTP request
     * @param httpChannel the channel that the HTTP request was received on
     * @param renderSupport the render support
     */
    public PageResourceRequest(URI resourceUri, Instant ifModifiedSince,
            HttpRequest httpRequest, IOSubchannel httpChannel,
            RenderSupport renderSupport) {
        super(resourceUri, ifModifiedSince,
            httpRequest, httpChannel, renderSupport);
    }

}
