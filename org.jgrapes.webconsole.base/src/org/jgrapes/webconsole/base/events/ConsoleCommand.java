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

package org.jgrapes.webconsole.base.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.JsonRpc;

/**
 * Events derived from this class are transformed to JSON messages
 * that are sent to the web console page. They may only be fired
 * on the {@link org.jgrapes.webconsole.base.ConsoleConnection#responsePipeline()}
 * (usually with 
 * {@link org.jgrapes.webconsole.base.ConsoleConnection#respond(org.jgrapes.core.Event)}).
 */
public abstract class ConsoleCommand extends Event<Void> {

    /** The mapper. */
    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ObjectMapper mapper
        = new ObjectMapper().findAndRegisterModules();

    /**
     * Emits the 
     * [JSON notification](https://www.jsonrpc.org/specification#notification)
     * using the given writer. Derived classes usually simply call 
     * {@link #emitJson(Writer, String, Object...)} with the method
     * name and parameters.
     * 
     * @param writer the writer
     */
    public abstract void emitJson(Writer writer)
            throws InterruptedException, IOException;

    /**
     * Creates a JSON notification from the given data.
     * Closes the `writer`.
     *
     * @param writer the writer
     * @param method the method
     * @param params the params
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void emitJson(Writer writer, String method, Object... params)
            throws IOException {
        JsonRpc rpc = new JsonRpc(method);
        if (params.length > 0) {
            for (Object obj : params) {
                rpc.addParam(obj);
            }
        }
        mapper.writeValue(writer, rpc);
        writer.flush();
    }

    /**
     * Calls {@link #emitJson(Writer, String, Object...)} with the
     * given method and parameters. Provided for backwards compatibility.
     *
     * @param writer the writer
     * @param method the method
     * @param params the params
     * @throws IOException Signals that an I/O exception has occurred.
     * @deprecated Use {@link #emitJson(Writer, String, Object...)} instead.
     */
    @Deprecated
    @SuppressWarnings("PMD.LinguisticNaming")
    protected void toJson(Writer writer, String method, Object... params)
            throws IOException {
        emitJson(writer, method, params);
    }
}
