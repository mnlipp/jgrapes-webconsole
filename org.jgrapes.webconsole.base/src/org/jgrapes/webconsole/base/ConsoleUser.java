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

package org.jgrapes.webconsole.base;

import java.security.Principal;
import java.util.Objects;

/**
 * A {@link Principal} representing an identity used by web console
 * components. Console users are usually not managed. Rather, the
 * information is derived from some authentication system and provided
 * to console components in a way that is independent from a particular
 * authentication strategy. 
 */
public class ConsoleUser implements Principal {

    private final String name;
    private final String displayName;

    /**
     * Instantiates a new console user.
     *
     * @param name the name
     * @param displayName the display name
     */
    public ConsoleUser(String name, String displayName) {
        super();
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * Instantiates a new console user with the name also being used 
     * as display name.
     *
     * @param name the name
     */
    public ConsoleUser(String name) {
        this(name, name);
    }

    /**
     * Gets the unique name. Used as key when storing or retrieving
     * user specific information.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * The name to use when showing the user to humans.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConsoleUser other = (ConsoleUser) obj;
        return Objects.equals(name, other.name);
    }

    /**
     * Returns the name followed by the display name in parenthesis.
     * 
     * @return the string
     */
    @Override
    public String toString() {
        return name + " (" + displayName + ")";
    }

}
