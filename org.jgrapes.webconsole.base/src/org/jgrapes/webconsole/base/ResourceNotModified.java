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
import java.time.Instant;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.events.Response;
import org.jgrapes.webconsole.base.events.ResourceRequest;

/**
 * Indicates that a resource provider found the resource to be
 * unmodified.
 */
public class ResourceNotModified extends ResourceResult {

    private final Instant lastModifiedAt;
    private final int maxAge;

    /**
     * Creates a new instance.
     *
     * @param request the request
     */
    public ResourceNotModified(ResourceRequest request, Instant lastModifiedAt,
            int maxAge) {
        super(request);
        this.lastModifiedAt = lastModifiedAt;
        this.maxAge = maxAge;
    }

    /** 
     * Send header only, because the resource does not have to be sent.
     */
    @Override
    public void process() throws IOException, InterruptedException {
        HttpResponse response = request().httpRequest().response().get();
        if (lastModifiedAt != null) {
            response.setField(HttpField.LAST_MODIFIED, lastModifiedAt);
        }
        ResponseCreationSupport.setMaxAge(response, maxAge);
        response.setStatus(HttpStatus.NOT_MODIFIED);
        request().httpChannel().respond(new Response(response));
    }

}
