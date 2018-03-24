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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jgrapes.portal.Portlet.RenderMode;
import org.jgrapes.portal.RenderSupport;


/**
 * Sent to the portal (server) if a new portlet instance of a given 
 * type should be added to the portal page. The portal server usually 
 * responds with a {@link RenderPortlet} event that has as payload the
 * HTML that displays the portlet on the portal page.
 * 
 * Properties may be passed with the event. The interpretation
 * of the properties is completely dependent on the handling portlet. 
 * 
 * The event's result is the portlet id of the new portlet instance.
 * 
 * ![Event Sequence](AddPortletRequestSeq.svg)
 * 
 * @startuml AddPortletRequestSeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "addPortlet"
 * activate Portal
 * Portal -> Portlet: AddPortletRequest
 * deactivate Portal
 * activate Portlet
 * Portlet -> Portal: RenderPortlet
 * deactivate Portlet
 * activate Portal
 * Portal -> Browser: "renderPortlet"
 * deactivate Portal
 * 
 * @enduml
 * 
 */
public class AddPortletRequest extends RenderPortletRequestBase<String> {

	private String portletType;
	private boolean foreground = true;
	private Map<? extends Object, ? extends Object> properties = null;
	
	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support
	 * @param portletType the type of the portlet
	 * @param mode the view mode that is requested
	 */
	public AddPortletRequest(RenderSupport renderSupport, String portletType,
	        RenderMode mode) {
		super(renderSupport, mode);
		this.portletType = portletType;
	}

	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support
	 * @param portletType the type of the portlet
	 * @param mode the view mode that is requested
	 * @param properties optional values for properties of the portlet instance
	 */
	public AddPortletRequest(RenderSupport renderSupport, String portletType,
	        RenderMode mode, Map<?,?> properties) {
		super(renderSupport, mode);
		this.portletType = portletType;
		@SuppressWarnings("unchecked")
		Map<Object, Object> props = (Map<Object,Object>)properties;
		this.properties = props;
	}

	/**
	 * Determines if the portlet will be put in the foreground.
	 * Defaults to `true` for added portlets as they are most likely
	 * supposed to be seen. 
	 *
	 * @param foreground the foreground
	 * @return the event for easy chaining
	 */
	public AddPortletRequest setForeground(boolean foreground) {
		this.foreground = foreground;
		return this;
	}
	
	/**
	 * Returns the portlet type
	 * 
	 * @return the portlet type
	 */
	public String portletType() {
		return portletType;
	}

	/**
	 * Returns the properties. Every event returns a mutable map,
	 * thus allowing event handlers to modify the map even if
	 * none was passed to the constructor.
	 */
	public Map<Object,Object> properties() {
		if (properties == null) {
			properties = new HashMap<>();
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> props = (Map<Object, Object>) properties;
		return props;
	}
	
	/**
	 * Convenience method for adding properties one-by-one.
	 * 
	 * @param key the property key
	 * @param value the property value
	 * @return the event for easy chaining
	 */
	public AddPortletRequest addPreference(Object key, Object value) {
		properties().put(key, value);
		return this;
	}
	
	/**
	 * Convenience method that performs the given action if a property
	 * with the given key exists.
	 * 
	 * @param key the property key
	 * @param action the action to perform
	 */
	public AddPortletRequest ifPresent(
			Object key, BiConsumer<Object,Object> action) {
		if (properties().containsKey(key)) {
			action.accept(key, properties().get(key));
		}
		return this;
	}

	@Override
	public boolean isForeground() {
		return foreground;
	}
	
}
