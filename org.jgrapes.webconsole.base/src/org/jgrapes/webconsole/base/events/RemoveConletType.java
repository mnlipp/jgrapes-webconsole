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

/**
 * Removes a web console component type from the console page.
 *
 * Implementations of the front-end are not expected to remove
 * resources from the page that were added by {@link AddConletType}
 * because this is hardly possible to implement.
 * 
 * The only expected action of the front-end is to prevent further
 * usage of the conlet, typically by removing the conlet type from 
 * e.g. a menu that allows adding conlets. The front-end is also
 * not expected to remove existing instances of the conlet. If
 * such a behavior is desired, the required {@link DeleteConlet}
 * events must be generated on the server side.
 */
public class RemoveConletType extends ConsoleCommand {

    private final String conletType;

    /**
     * Create a new event for the given web console component type.
     * 
     * @param conletType a unique id for the web console component type 
     * (usually the class name)
     */
    public RemoveConletType(String conletType) {
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

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "removeConletType", conletType());
    }
}
