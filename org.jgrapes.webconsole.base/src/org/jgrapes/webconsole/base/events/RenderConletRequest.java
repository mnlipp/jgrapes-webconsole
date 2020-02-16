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

import java.util.Set;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * Sent to the web console (server) if an existing web console component 
 * instance should be updated. The web console server usually responds with 
 * a {@link RenderConlet} event that has as payload the
 * HTML that displays the web console component on the web console page.
 * 
 * ![Event Sequence](RenderConletRequestSeq.svg)
 * 
 * The event's result must be set to `true` by the rendering 
 * web console component.
 * 
 * @startuml RenderConletRequestSeq.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "renderConletRequest"
 * activate WebConsole
 * WebConsole -> Conlet: RenderConletRequest
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> WebConsole: RenderConlet
 * deactivate Conlet
 * activate WebConsole
 * WebConsole -> Browser: "renderConlet"
 * deactivate WebConsole
 * 
 * @enduml
 */
public class RenderConletRequest
        extends RenderConletRequestBase<Boolean> {

    private final String conletId;

    /**
     * Creates a new request.
     *
     * @param renderSupport the render support for generating the response
     * @param conletId the web console component to be updated
     * @param renderModes the render options
     */
    public RenderConletRequest(RenderSupport renderSupport,
            String conletId, Set<RenderMode> renderModes) {
        super(renderSupport, renderModes);
        this.conletId = conletId;
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
     * Checks if the web console component has been rendered (i.e. the 
     * event has been handled).
     *
     * @return true, if successful
     */
    public boolean hasBeenRendered() {
        return !currentResults().isEmpty() && currentResults().get(0);
    }
}
