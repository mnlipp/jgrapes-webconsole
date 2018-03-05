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

package org.jgrapes.portal.events;

import java.io.IOException;
import java.io.Writer;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.jdrupes.json.JsonBeanEncoder;
import org.jgrapes.core.Event;

/**
 * Events derived from this class are transformed to JSON messages
 * that are sent to the portal session. They may only be fired
 * on the {@link org.jgrapes.portal.PortalSession#responsePipeline()}
 * (usually with 
 * {@link org.jgrapes.portal.PortalSession#respond(org.jgrapes.core.Event)}).
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
	 */
	protected void toJson(Writer writer, String method, Object... params) {
		JsonGenerator generator = Json.createGenerator(writer);
		generator.writeStartObject();
		generator.write("jsonrpc", "2.0");
		generator.write("method", method);
		if (params.length > 0) {
			generator.writeKey("params");
			generator.writeStartArray();
			for (Object obj: params) {
				if (obj == null) {
					generator.writeNull();
					continue;
				}
				if (obj instanceof JsonValue) {
					generator.write((JsonValue)obj);
					continue;
				}
				JsonBeanEncoder.create(generator).writeObject(obj);
			}
			generator.writeEnd();
		}
		generator.writeEnd();
		generator.close();
	}
}
