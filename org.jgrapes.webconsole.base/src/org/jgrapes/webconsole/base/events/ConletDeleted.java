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
import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * A notification that a conlet view was deleted in the browser.
 */
public class ConletDeleted extends Event<Void> {

    private final RenderSupport renderSupport;
    private final String conletId;
    private final Set<RenderMode> renderModes;

    /**
     * Creates a new event.
     *
     * @param renderSupport the render support from the web console in case
     * the response requires it
     * @param conletId the web console component model that the notification is
     * directed at
     * @param renderModes the kind of views that have been deleted. If
     * empty, the last view has been deleted, i.e. the conlet is no longer
     * used in the broweser.
     */
    public ConletDeleted(RenderSupport renderSupport,
            String conletId, Set<RenderMode> renderModes) {
        this.renderSupport = renderSupport;
        this.conletId = conletId;
        this.renderModes = renderModes;
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
     * Returns the render modes that have been deleted. An empty
     * set indicates that all views have been deleted, i.e.
     * the conlet is no longer used in the browser.
     *
     * @return the render modes
     */
    public Set<RenderMode> renderModes() {
        return renderModes;
    }
}
