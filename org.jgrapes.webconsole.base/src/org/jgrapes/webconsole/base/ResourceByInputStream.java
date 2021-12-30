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
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.webconsole.base.events.ResourceRequest;

/**
 * Returns an {@link OutputStream} as result.
 */
public class ResourceByInputStream extends ResourceResult {

    private final InputStream stream;
    private final MediaType mediaType;
    private final Instant lastModifiedAt;
    private final int maxAge;

    /**
     * Instantiates a result that is provided by an {@link OutputStream}.
     *
     * @param request the request
     * @param stream the stream
     * @param mediaType the media type, may be `null`
     * @param lastModifiedAt the last modified at
     * @param maxAge the max age
     */
    public ResourceByInputStream(ResourceRequest request,
            InputStream stream, MediaType mediaType,
            Instant lastModifiedAt, int maxAge) {
        super(request);
        this.stream = stream;
        this.mediaType = mediaType;
        this.lastModifiedAt = lastModifiedAt;
        this.maxAge = maxAge;
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void process() throws IOException, InterruptedException {
        if (stream == null) {
            ResponseCreationSupport.sendResponse(request().httpRequest(),
                request().httpChannel(), HttpStatus.NOT_FOUND);
            return;
        }

        HttpResponse response = request().httpRequest().response().get();
        if (lastModifiedAt != null) {
            response.setField(HttpField.LAST_MODIFIED, lastModifiedAt);
        }
        if (mediaType != null) {
            response.setContentType(mediaType);
        } else {
            // Usually a set by setContentType
            response.setHasPayload(true);
        }
        ResponseCreationSupport.setMaxAge(response, maxAge);
        response.setStatus(HttpStatus.OK);
        request().httpChannel().respond(new Response(response));
        // Start sending content
        new InputStreamPipeline(stream, request().httpChannel()).run();
    }

}
