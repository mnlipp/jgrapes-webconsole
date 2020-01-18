/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

import java.io.IOException;
import java.net.URL;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.webconsole.base.events.ResourceRequest;

/**
 * Returns a {@link URL} as result. The result will be processed by
 * checking if the resource needs to be sent and, if this is the case,
 * sending it.
 */
public class ResourceByUrl extends ResourceResult {
    private final URL resourceUrl;

    /**
     * Instantiates a new result represented by a {@link URL}.
     *
     * @param request the request
     * @param resourceUrl the resource url
     */
    public ResourceByUrl(ResourceRequest request, URL resourceUrl) {
        super(request);
        this.resourceUrl = resourceUrl;
    }

    @Override
    public void process() throws IOException, InterruptedException {
        if (resourceUrl == null) {
            ResponseCreationSupport.sendResponse(request().httpRequest(),
                request().httpChannel(), HttpStatus.NOT_FOUND);
            return;
        }

        // Send resource
        ResponseCreationSupport.sendStaticContent(request().httpRequest(),
            request().httpChannel(), path -> resourceUrl, null);
    }

}
