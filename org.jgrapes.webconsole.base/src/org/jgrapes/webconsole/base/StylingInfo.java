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

package org.jgrapes.webconsole.base;

import java.util.Map;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;

/**
 * Evaluates and provides the styling info to a web console component.
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
     * a component of type {@link ConsoleWeblet} and returns 
     * the result of invoking {@link ConsoleWeblet#styling()}.
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
            // Try to get the information from the web console (weblet)
            ComponentType console = component;
            while (true) {
                console = Components.manager(console).parent();
                if (console == null) {
                    break;
                }
                // Component is in tree, cache result.
                styling = "standard";
                if (console instanceof ConsoleWeblet) {
                    styling = ((ConsoleWeblet) console).styling();
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
