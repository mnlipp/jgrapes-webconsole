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
import org.jgrapes.webconsole.base.events.ResourceRequest;

/**
 * The base class for all results from processing a resource
 * request.
 */
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
