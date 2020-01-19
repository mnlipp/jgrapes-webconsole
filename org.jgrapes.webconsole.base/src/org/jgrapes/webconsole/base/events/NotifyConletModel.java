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

import org.jdrupes.json.JsonArray;
import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * A decoded notification (as defined by the JSON RPC specification) that
 * invokes a method on a web console component model. Usually, though 
 * not necessarily, the web console component component responds by sending a
 * {@link NotifyConletView} to update the web console component representation.
 * 
 * ![Event Sequence](NotifyConletModelSeq.svg)
 * 
 * @startuml NotifyConletModelSeq.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "notifyConletModel"
 * activate WebConsole
 * WebConsole -> Conlet: NotifyConletModel
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> WebConsole: NotifyConletView
 * deactivate Conlet
 * activate WebConsole
 * WebConsole -> Browser: "notifyConletView"
 * deactivate WebConsole
 * 
 * @enduml
 */
@SuppressWarnings("PMD.DataClass")
public class NotifyConletModel extends Event<Void> {

    private final RenderSupport renderSupport;
    private final String conletId;
    private final String method;
    private final JsonArray params;

    /**
     * Creates a new event.
     * 
     * @param renderSupport the render support from the portal in case
     * the response requires it
     * @param conletId the web console component model that the notification 
     * is directed at 
     * @param method the method to be executed
     * @param params parameters
     */
    public NotifyConletModel(RenderSupport renderSupport,
            String conletId, String method, JsonArray params) {
        this.renderSupport = renderSupport;
        this.conletId = conletId;
        this.method = method;
        this.params = params;
    }

    /**
     * Returns the render support.
     * 
     * @return the render support
     */
    public RenderSupport renderSupport() {
        return renderSupport;
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
     * Returns the method.
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
    public JsonArray params() {
        return params;
    }
}
