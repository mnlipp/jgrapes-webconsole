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

package org.jgrapes.webconsole.base.events;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.jgrapes.core.Event;

/**
 * Sent to a web console component to update some of its properties. 
 * The interpretation of the properties is completely dependent on the 
 * handling web console component.
 * 
 * This event has a close relationship to the {@link NotifyConletModel}
 * event. The latter is used by web console component's functions to 
 * send information from the console page to the web console component 
 * model. The interpretation of this information is only known by the web
 * console component. The {@link UpdateConletModel} event should be 
 * used to to pass information within the application, i.e. on the server
 * side.
 * 
 * Depending on the information passed, it may be good practice to 
 * write an event handler for the web console component that converts a 
 * {@link NotifyConletModel} to a {@link UpdateConletModel} that is
 * fired on its channel instead of handling it immediately. This allows 
 * events sent from the console page and from other components in the 
 * application to be handled in a uniform way.
 */
public class UpdateConletModel extends Event<Void> {

    private String conletId;
    private Map<Object, Object> properties;

    /**
     * Creates a new event.
     * 
     * @param conletId the id of the web console component
     * @param properties the properties to update
     */
    public UpdateConletModel(String conletId, Map<?, ?> properties) {
        this.conletId = conletId;
        @SuppressWarnings("unchecked")
        Map<Object, Object> props = (Map<Object, Object>) properties;
        this.properties = props;
    }

    /**
     * Creates a new event. This constructor creates an empty map of
     * properties and is therefore intended to be used together with
     * {@link #addPreference(Object, Object)}.
     *
     * @param conletId the web console component id
     */
    public UpdateConletModel(String conletId) {
        this(conletId, new HashMap<>());
    }

    /**
     * Returns the web console component id.
     * 
     * @return the web console component id
     */
    public String conletId() {
        return conletId;
    }

    /**
     * Returns the properties. Every event returns a mutable map,
     * thus allowing event handlers to modify the map even if
     * none was passed to the constructor.
     */
    public Map<Object, Object> properties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    /**
     * Convenience method for adding properties one-by-one.
     * 
     * @param key the property key
     * @param value the property value
     * @return the event for easy chaining
     */
    public UpdateConletModel addPreference(Object key, Object value) {
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
    public UpdateConletModel ifPresent(
            Object key, BiConsumer<Object, Object> action) {
        if (properties().containsKey(key)) {
            action.accept(key, properties().get(key));
        }
        return this;
    }
}
