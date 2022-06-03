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

import org.jgrapes.core.Associator;
import org.jgrapes.core.Event;

/**
 * A notification within the server side that an item that is expected 
 * to be associated with a channel is missing or not in a usable state. 
 * The item specification is usually the key used by the {@link Associator}. 
 * 
 * This event may be fired by components that require the item and 
 * should be handled by the component that is responsible for 
 * providing the item.
 * 
 * @see ResourceUpdated
 */
@SuppressWarnings("PMD.DataClass")
public class ResourceNotAvailable extends Event<Void> {

    private final Object itemSpecification;

    /**
     * Creates a new event.
     *
     * @param itemSpecification information about the item, usually the 
     * item's class.
     */
    public ResourceNotAvailable(Object itemSpecification) {
        this.itemSpecification = itemSpecification;
    }

    /**
     * Return the information that specifies the item. 
     *
     * @return the item specification
     */
    public Object itemSpecification() {
        return itemSpecification;
    }
}
