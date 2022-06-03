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
 * A notification within the server side that an item has been updated.
 * The item specification is usually the key used by the {@link Associator}.
 * 
 * This event should be fired by the component that provides the item and 
 * may be handled by components that use information provided by the item.
 * 
 * @see ResourceNotAvailable
 */
@SuppressWarnings("PMD.DataClass")
public class ResourceUpdated extends Event<Void> {

    private final Object itemSpecification;

    /**
     * Creates a new event.
     *
     * @param itemSpecification information about the item, 
     * usually the item's class.
     */
    public ResourceUpdated(Object itemSpecification) {
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
