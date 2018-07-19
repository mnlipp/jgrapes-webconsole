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

import org.jgrapes.portal.events.ResourceRequest;

/**
 * The base class for all results from processing a resource
 * request.
 */
@Deprecated
public abstract class ResourceResult {

    private final ResourceRequest request;

    /**
     * Instantiates a new resource result.
     *
     * @param request the request
     */
    public ResourceResult(ResourceRequest request) {
        this.request = request;
    }

    protected ResourceRequest request() {
        return request;
    }

    /**
     * Specifies hoe the provided resource is processed.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public abstract void process() throws IOException, InterruptedException;
}
