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

import java.util.Locale;

import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.RenderSupport;

/**
 * Signals that the locale for the portal has changed. Should be handled
 * by portlets that support localization by updating the representation.
 * 
 * ![Event Sequence](SetLocale.svg)
 * 
 * @startuml SetLocale.svg
 * hide footbox
 * 
 * Browser -> WebConsole: "setLocale"
 * activate WebConsole
 * activate ConsoleWeblet
 * WebConsole -> ConsoleWeblet: SetLocale
 * deactivate ConsoleWeblet
 * loop for portlets with l10n support
 *     WebConsole -> PortletComponent: SetLocale
 * end loop
 * deactivate WebConsole
 * actor Framework
 * Framework -> ConsoleWeblet: SetLocaleDone
 * activate ConsoleWeblet
 * opt reload requested
 *     ConsoleWeblet -> Browser: "reload"
 * end
 * deactivate ConsoleWeblet
 * 
 * @enduml
 */
public class SetLocale extends Event<Void> {

    private RenderSupport renderSupport;
    private final Locale locale;
    private boolean reload;

    /**
     * Creates a new event.
     *
     * @param renderSupport the render support
     * @param locale the locale to set
     * @param reload the reload
     */
    public SetLocale(RenderSupport renderSupport, Locale locale,
            boolean reload) {
        this.renderSupport = renderSupport;
        this.locale = locale;
        this.reload = reload;
        new SetLocaleCompleted(this);
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
     * Returns the locale to set.
     * 
     * @return the locale
     */
    public Locale locale() {
        return locale;
    }

    /**
     * Returns `true` if the portal needs to be reloaded after
     * changing the locale.
     *
     * @return true, if reload is required
     */
    public boolean reload() {
        return reload;
    }

    /**
     * Sets the reload flag. Used by portlets that cannot dynamically
     * update their content to the new locale.
     * 
     * For optimized behavior, portlets should check {@link #reload()}
     * before generating events that update the content dynamically.
     * Portlets that invoke this method 
     * should define a handler with a higher priority. 
     */
    public void forceReload() {
        this.reload = true;
    }

}
