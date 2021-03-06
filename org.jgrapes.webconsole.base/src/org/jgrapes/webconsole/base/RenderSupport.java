/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import java.net.URI;
import org.jgrapes.webconsole.base.events.ConletResourceRequest;
import org.jgrapes.webconsole.base.events.PageResourceRequest;

/**
 * Provides support for creating URIs in the web console scope that
 * are forwarded to components listening on the web console channel.  
 */
public interface RenderSupport {

    /**
     * Returns the web console library URI.
     *
     * @param uri the uri
     * @return the uri
     */
    URI consoleBaseResource(URI uri);

    /**
     * Convenience method that converts the path to an URI
     * before calling {@link #consoleBaseResource(URI)}.
     * 
     * @param path the path 
     * @return the resulting URI
     */
    default URI consoleBaseResource(String path) {
        return consoleBaseResource(WebConsoleUtils.uriFromPath(path));
    }

    /**
     * Create a reference to a resource provided by the web console.
     * 
     * @param uri the URI  
     * @return the resulting URI
     */
    URI consoleResource(URI uri);

    /**
     * Convenience method that converts the path to an URI
     * before calling {@link #consoleResource(URI)}.
     * 
     * @param path the path 
     * @return the resulting URI
     */
    default URI consoleResource(String path) {
        return consoleResource(WebConsoleUtils.uriFromPath(path));
    }

    /**
     * Create a reference to a resource provided by a page resource
     * provider. Requesting the resulting URI results in a 
     * {@link PageResourceRequest}.
     * 
     * @param uri the URI made available as
     * {@link PageResourceRequest#resourceUri()}  
     * @return the resulting URI
     */
    URI pageResource(URI uri);

    /**
     * Convenience method that converts the path to an URI
     * before calling {@link #pageResource(URI)}.
     * 
     * @param path the path 
     * @return the resulting URI
     */
    default URI pageResource(String path) {
        return pageResource(WebConsoleUtils.uriFromPath(path));
    }

    /**
     * Create a reference to a resource provided by a web console component
     * of the given type. Requesting the resulting URI results
     * in a {@link ConletResourceRequest}.
     * 
     * @param conletType the web console component type
     * @param uri the URI made available as 
     * {@link ConletResourceRequest#resourceUri()}
     * @return the resulting URI
     */
    URI conletResource(String conletType, URI uri);

    /**
     * Convenience method that converts the path to an URI
     * before calling {@link #conletResource(String, URI)}.
     * 
     * @param conletType the web console component type
     * @param path the path 
     * @return the resulting URI
     */
    default URI conletResource(String conletType, String path) {
        return conletResource(conletType, WebConsoleUtils.uriFromPath(path));
    }

    /**
     * Indicates if minified resources should be used.
     * 
     * @return the setting
     */
    boolean useMinifiedResources();
}
