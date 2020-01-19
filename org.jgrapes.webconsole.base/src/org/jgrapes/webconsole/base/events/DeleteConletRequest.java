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

import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * A request to delete a web console component instance.
 */
public class DeleteConletRequest extends Event<Void> {

    private final RenderSupport renderSupport;
    private final String conletId;

    /**
     * Creates a new event.
     * 
     * @param renderSupport the render support from the web console in case
     * the response requires it
     * @param conletId the web console component model that the notification is 
     * directed at 
     */
    public DeleteConletRequest(RenderSupport renderSupport,
            String conletId) {
        this.renderSupport = renderSupport;
        this.conletId = conletId;
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

}
