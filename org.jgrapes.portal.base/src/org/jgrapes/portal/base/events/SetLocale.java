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

package org.jgrapes.portal.base.events;

import java.util.Locale;

import org.jgrapes.core.Event;
import org.jgrapes.portal.base.PortalWeblet;

/**
 * Signals that the locale for the portal has changed. This  event is
 * handled by the {@link PortalWeblet} but may, of course, also be
 * used by other components.
 * 
 * ![Event Sequence](SetLocale.svg)
 * 
 * @startuml SetLocale.svg
 * hide footbox
 * 
 * Browser -> Portal: "setLocale"
 * activate Portal
 * Portal -> PortalWeblet: SetLocale
 * deactivate Portal
 * activate PortalWeblet
 * opt reload requested
 *     PortalWeblet -> Browser: "reload"
 * end
 * deactivate PortalWeblet
 * 
 * @enduml
 */
public class SetLocale extends Event<Void> {

    private final Locale locale;
    private final boolean reload;

    /**
     * Creates a new event.
     * 
     * @param locale the locale to set
     */
    public SetLocale(Locale locale, boolean reload) {
        this.locale = locale;
        this.reload = reload;
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

}
