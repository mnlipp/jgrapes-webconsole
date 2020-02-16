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

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import org.jgrapes.webconsole.base.Conlet.RenderMode;

/**
 * Request the browser to remove a conlet view from the display.
 */
public class DeleteConlet extends ConsoleCommand {

    private final String conletId;
    private final Set<RenderMode> renderModes;

    /**
     * Creates a new event.
     *
     * @param conletId the web console component (view) that should be deleted
     * @param renderModes the views to delete. If empty, all views should
     * be deleted, i.e. the conlet may no longer be used in the browser. 
     */
    public DeleteConlet(String conletId, Set<RenderMode> renderModes) {
        this.conletId = conletId;
        this.renderModes = renderModes;
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
     * Returns the render modes that should be deleted. An empty
     * set indicates that all views should be deleted, i.e.
     * the conlet is no longer used in the browser.
     *
     * @return the render modes
     */
    public Set<RenderMode> renderModes() {
        return renderModes;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "deleteConlet", conletId(),
            renderModes.stream().map(RenderMode::name)
                .toArray(size -> new String[size]));
    }

}
