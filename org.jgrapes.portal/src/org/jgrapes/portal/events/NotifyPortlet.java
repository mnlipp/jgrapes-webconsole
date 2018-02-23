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

import java.io.Writer;

/**
 * A notification (as defined by the JSON RPC specification) to be sent to
 * the portlet view (the browser).
 */
public class NotifyPortlet extends PortalCommand {

	private String portletType;
	private String portletId;
	private String method;
	private Object[] params;
	
	/**
	 * Creates a new event.
	 *  
	 * @param portletType the portlet type (used by the portal 
	 * core JS to look up the available functions, see {@link AddPortletType})
	 * @param portletId the portlet (view) instance that the 
	 * notification is directed at
	 * @param method the method (function) to be executed, must
	 * have been registered by handling {@link AddPortletType}
	 * @param params the parameters
	 */
	public NotifyPortlet(String portletType,
	        String portletId, String method, Object... params) {
		this.portletType = portletType;
		this.portletId = portletId;
		this.method = method;
		this.params = params;
	}

	/**
	 * Returns the portlet class.
	 * 
	 * @return the portlet class
	 */
	public String portletType() {
		return portletType;
	}

	/**
	 * Returns the portlet id.
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * Returns the method to be executed.
	 * 
	 * @return the method
	 */
	public String method() {
		return method;
	}

	/**
	 * Returns the parameters.
	 * 
	 * @return the parameters
	 */
	public Object[] params() {
		return params;
	}

	@Override
	public void toJson(Writer writer) {
		toJson(writer, "notifyPortletView", portletType(), portletId(), 
				method(), params());
	}
}
