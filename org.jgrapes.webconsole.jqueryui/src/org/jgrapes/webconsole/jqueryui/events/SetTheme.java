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

package org.jgrapes.webconsole.jqueryui.events;

import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.ConsoleWeblet;

/**
 * Signals that the theme for the web console has changed. This  event is
 * handled by the {@link ConsoleWeblet} but may, of course, also be
 * used by other components.
 * 
 * ![Event Sequence](SetTheme.svg)
 * 
 * @startuml SetTheme.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "settheme"
 * activate WebConsole
 * WebConsole -> ConsoleWeblet: SetTheme
 * deactivate WebConsole
 * activate ConsoleWeblet
 * ConsoleWeblet -> Browser: "reload"
 * deactivate ConsoleWeblet
 * 
 * @enduml
 */
public class SetTheme extends Event<Void> {

    private final String theme;

    /**
     * Creates a new event.
     * 
     * @param theme the theme to set
     */
    public SetTheme(String theme) {
        this.theme = theme;
    }

    /**
     * Returns the theme to set.
     * 
     * @return the theme
     */
    public String theme() {
        return theme;
    }
}
