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
import java.util.Set;
import java.util.function.BiConsumer;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * Sent to the console (server) if a new web console component instance 
 * of a given type should be added to the web console page. The console 
 * server usually responds with a {@link RenderConlet} event that has as
 * payload the HTML that displays the web console component on the console
 * page.
 * 
 * Properties may be passed with the event. The interpretation
 * of the properties is completely dependent on the web console
 * component that handles the request. It is recommended to use 
 * {@link String}s as keys and JDK types as values. This avoids 
 * classpath dependencies on the web console component that is 
 * to be added. 
 * 
 * {@link AddConletRequest} can also be generated on the server side
 * to automatically add a conlet in response to some event. Usually,
 * the origin of the event is not important when handling the event.
 * Nevertheless, the origin can be determined by calling
 * {@link #isFrontendRequest()} as it may be important e.g. for 
 * security related checks.
 * 
 * The event's result is the web console component id of the new 
 * web console component instance.
 * 
 * ![Event Sequence](AddConletRequestSeq.svg)
 * 
 * @startuml AddConletRequestSeq.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "addConlet"
 * activate WebConsole
 * WebConsole -> Conlet: AddConletRequest
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> WebConsole: RenderConlet
 * deactivate Conlet
 * activate WebConsole
 * WebConsole -> Browser: "renderConlet"
 * deactivate WebConsole
 * 
 * @enduml
 * 
 */
public class AddConletRequest extends RenderConletRequestBase<String> {

    private final String conletType;
    private Map<? extends Object, ? extends Object> properties;
    private boolean frontendRequest;

    /**
     * Creates a new event.
     *
     * @param renderSupport the render support
     * @param conletType the type of the web console component
     * @param renderModes the render modes
     */
    public AddConletRequest(RenderSupport renderSupport, String conletType,
            Set<RenderMode> renderModes) {
        super(renderSupport, renderModes);
        this.conletType = conletType;
        this.properties = new HashMap<>();
    }

    /**
     * Creates a new event.
     *
     * @param renderSupport the render support
     * @param conletType the type of the web console component
     * @param renderModes the render modes
     * @param properties optional values for properties of the 
     * web console component instance
     */
    public AddConletRequest(RenderSupport renderSupport, String conletType,
            Set<RenderMode> renderModes, Map<?, ?> properties) {
        this(renderSupport, conletType, renderModes);
        this.properties = new HashMap<>(properties);
    }

    /**
     * Marks this event as originating from the browser.
     *
     * @return the adds the conlet request
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public AddConletRequest setFrontendRequest() {
        frontendRequest = true;
        return this;
    }

    /**
     * Checks if this request originated from the browser.
     *
     * @return true, if is frontend request
     */
    public boolean isFrontendRequest() {
        return frontendRequest;
    }

    /**
     * Returns the web console component type
     * 
     * @return the web console component type
     */
    public String conletType() {
        return conletType;
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
    public AddConletRequest addProperty(Object key, Object value) {
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
    public AddConletRequest ifPresent(
            Object key, BiConsumer<Object, Object> action) {
        if (properties().containsKey(key)) {
            action.accept(key, properties().get(key));
        }
        return this;
    }
}
