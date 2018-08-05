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

package org.jgrapes.portal.base.events;

import java.io.IOException;
import java.io.Writer;

import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonRpc;
import org.jgrapes.core.Event;

/**
 * Events derived from this class are transformed to JSON messages
 * that are sent to the portal session. They may only be fired
 * on the {@link org.jgrapes.portal.base.PortalSession#responsePipeline()}
 * (usually with 
 * {@link org.jgrapes.portal.base.PortalSession#respond(org.jgrapes.core.Event)}).
 */
public abstract class PortalCommand extends Event<Void> {

    /**
     * Writes the event as JSON notification to the given writer.
     * Derived classes usually simply call 
     * {@link #toJson(Writer, String, Object...)} with the method
     * name and parameters.
     * 
     * @param writer the writer
     */
    public abstract void toJson(Writer writer)
            throws InterruptedException, IOException;

    /**
     * Creates a JSON notification from the given data.
     * Closes the `writer`.
     * 
     * @param writer the writer
     * @throws IOException 
     */
    protected void toJson(Writer writer, String method, Object... params)
            throws IOException {
        JsonRpc rpc = JsonRpc.create();
        rpc.setMethod(method);
        if (params.length > 0) {
            for (Object obj : params) {
                rpc.addParam(obj);
            }
        }
        JsonBeanEncoder.create(writer).writeObject(rpc).flush();
    }
}
