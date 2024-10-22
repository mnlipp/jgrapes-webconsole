/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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
import java.util.HashSet;
import java.util.Set;
import org.jgrapes.webconsole.base.Conlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;

/**
 * Inform the front-end about changes of a conlet type.
 * 
 * The only supported change is a modification of the render
 * modes offered to the user (see {@link AddConletType#addRenderMode}.
 */
public class UpdateConletType extends ConsoleCommand {

    private final String conletType;
    private final Set<Conlet.RenderMode> renderModes = new HashSet<>();

    /**
     * Create a new event for the given web console component type.
     * 
     * @param conletType a unique id for the web console component type 
     * (usually the class name)
     */
    public UpdateConletType(String conletType) {
        this.conletType = conletType;
    }

    /**
     * Return the web console component type.
     * 
     * @return the web console component type
     */
    public String conletType() {
        return conletType;
    }

    /**
     * Add a render mode.
     *
     * @param mode the mode
     * @return the event for easy chaining
     */
    public UpdateConletType addRenderMode(RenderMode mode) {
        renderModes.add(mode);
        return this;
    }

    /**
     * Return the render modes.
     * 
     * @return the result
     */
    public Set<RenderMode> renderModes() {
        return renderModes;
    }

    @Override
    public void emitJson(Writer writer) throws IOException {
        emitJson(writer, "updateConletType", conletType(),
            renderModes().stream().map(RenderMode::name)
                .toArray(size -> new String[size]));
    }
}
