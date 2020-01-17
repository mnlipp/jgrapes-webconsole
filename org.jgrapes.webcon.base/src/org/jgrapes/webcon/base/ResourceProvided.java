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

package org.jgrapes.webcon.base;

import java.io.IOException;

import org.jgrapes.webcon.base.events.ResourceRequest;

/**
 * Indicates that a resource provider has fully processed the
 * resource request a provided a response.
 */
public class ResourceProvided extends ResourceResult {

    /**
     * Creates a new instance.
     *
     * @param request the request
     */
    public ResourceProvided(ResourceRequest request) {
        super(request);
    }

    /** 
     * Does nothing, because the resource has already been sent.
     */
    @Override
    public void process() throws IOException, InterruptedException {
        // Do nothing.
    }
}
