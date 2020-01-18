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

import org.jgrapes.core.CompletionEvent;
import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * Signals the successful loading of the portal structure
 * in the browser. Portlets should respond to this event by
 * adding their types (using {@link AddConletType} events).
 * 
 * After the portal ready event has been processed, a {@link ConsolePrepared}
 * event is generated as its {@link CompletionEvent}. All further action is 
 * triggered by the {@link ConsolePrepared} event.
 */
public class ConsoleReady extends Event<Void> {

    private final RenderSupport renderSupport;

    /**
     * Creates a new event.
     * 
     * @param renderSupport the render support for generating responses
     */
    public ConsoleReady(RenderSupport renderSupport) {
        new ConsolePrepared(this);
        this.renderSupport = renderSupport;
    }

    /**
     * Returns the render support.
     * 
     * @return the render support
     */
    public RenderSupport renderSupport() {
        return renderSupport;
    }
}
