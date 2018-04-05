/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Causes a notification to be display on the top of the portal page.
 * 
 * The event triggers the creation of a notification widget in the
 * portal page. 
 */
public class DisplayNotification extends PortalCommand {

	private String content;
	private Map<String,Object> options;
	
	/**
	 * Creates a new event. The content must be valid HTML, i.e. it
	 * must start with a tag (usually a "`<span>`"). See the JavaScript 
	 * documentation of the
	 * <a href="../../../../jsdoc/index.html#notificationplugin">
	 * notification plugin</a> for details about the available options.
	 * 
	 * @param content the content (valid HTML)
	 * @param options the options (must be serializable as JSON)
	 */
	public DisplayNotification(String content, Map<String,Object> options) {
		this.content = content;
		this.options = options;
	}

	/**
	 * Creates a new event without any options.
	 * The content must be valid HTML, i.e. it
	 * must start with a tag (usually a "`<span>`").
	 * 
	 * @param content the content (valid HTML)
	 */
	public DisplayNotification(String content) {
		this(content, null);
	}

	/**
	 * Adds an option to the event. See the JavaScript documentation of the
	 * <a href="../../../../jsdoc/index.html#notificationplugin">
	 * notification plugin</a> for details about the available options.
	 * 
	 * @param name the option name
	 * @param value the option value (must be serializable as JSON)
	 * @return the event for easy chaining
	 */
	public DisplayNotification addOption(String name, Object value) {
		if (options == null) {
			options = new HashMap<String, Object>();
		}
		options.put(name, value);
		return this;
	}
	
	/**
	 * Returns the content.
	 * 
	 * @return the content
	 */
	public String content() {
		return content;
	}

	/**
	 * Return the options.
	 * 
	 * @return the options
	 */
	public Map<String,Object> options() {
		return options == null ? Collections.emptyMap() : options;
	}

	@Override
	public void toJson(Writer writer) throws IOException {
		Map<String,Object> options = options();
		options.put("destroyOnClose", true);
		toJson(writer, "displayNotification", content(), options);
	}
	
}
