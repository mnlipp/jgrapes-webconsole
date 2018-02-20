/*
 * JGrapes Event Driven Framework
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

package org.jgrapes.portal;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.portal.events.ResourceRequest;

/**
 * Returns a {@link Runnable} that writes to an {@link OutputStream} as result.
 */
public class ResourceByGenerator extends ResourceResult {

	private Generator generator;
	private MediaType mediaType;
	private Instant lastModifiedAt;
	private int maxAge;

	public interface Generator {
		void write(OutputStream out) throws IOException;
	}
	
	/**
	 * Instantiates a result that is provided by an {@link OutputStream}.
	 *
	 * @param request the request
	 * @param generator the generator
	 * @param mediaType the media type
	 * @param lastModifiedAt the last modified at
	 * @param maxAge the max age
	 */
	public ResourceByGenerator(ResourceRequest request, 
			Generator generator, MediaType mediaType, 
			Instant lastModifiedAt, int maxAge) {
		super(request);
		this.generator = generator;
		this.mediaType = mediaType;
		this.lastModifiedAt = lastModifiedAt;
		this.maxAge = maxAge;
	}

	@Override
	public void process() throws IOException, InterruptedException {
		if (generator == null) {
			ResponseCreationSupport.sendResponse(request().httpRequest(), 
					request().httpChannel(), HttpStatus.NOT_FOUND);
			return;
		}
		
		HttpResponse response = request().httpRequest().response().get();
		if (lastModifiedAt != null) {
			response.setField(HttpField.LAST_MODIFIED, lastModifiedAt);
		}
		response.setContentType(mediaType);
		ResponseCreationSupport.setMaxAge(response, (rq, mt) -> maxAge,
				request().httpRequest(), mediaType);
		response.setStatus(HttpStatus.OK);
		request().httpChannel().respond(new Response(response));
		// Start sending content
		try (@SuppressWarnings("resource")
		OutputStream out = new ByteBufferOutputStream(
				request().httpChannel()).suppressClose()) {
			generator.write(out);
		}
	}
	
}
