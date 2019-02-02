/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.portal.base;

import java.util.Map;

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;

/**
 * Evaluates and provides the styling info to a portal component.
 */
public class StylingInfo {

    private final ComponentType component;
    private final Map<Object, Object> properties;
    private String styling;

    /**
     * Instantiates a new styling info.
     *
     * @param component the component
     * @param properties the properties
     */
    public StylingInfo(ComponentType component,
            Map<Object, Object> properties) {
        super();
        this.component = component;
        this.properties = properties;
    }

    /**
     * Returns the styling information. The method looks for an entry
     * with key "style" in properties first. If no entry is found,
     * it searches through the anchestors of this component for
     * a component of type {@link PortalWeblet} and returns 
     * the result of invoking {@link PortalWeblet#styling()}.
     * Else it returns "standard".
     *
     * @return the result
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public String get() {
        if (styling != null) {
            return styling;
        }
        if (properties != null) {
            styling = (String) properties.get("styling");
        }
        if (styling == null) {
            // Try to get the information from the portal (weblet)
            ComponentType portal = component;
            while (true) {
                portal = Components.manager(portal).parent();
                if (portal == null) {
                    break;
                }
                // Component is in tree, cache result.
                styling = "standard";
                if (portal instanceof PortalWeblet) {
                    styling = ((PortalWeblet) portal).styling();
                    break;
                }
            }
        }
        if (styling == null) {
            return "standard";
        }
        return styling;
    }

}
