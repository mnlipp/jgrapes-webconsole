/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2020 Michael N. Lipp
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

import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;
import org.jgrapes.webconsole.base.ConsoleWeblet;

/**
 * Indicates that the processing of a {@link SetLocale} event has been 
 * completed.
 * 
 * This  event is handled by the {@link ConsoleWeblet} which
 * checks if a reload is required and triggers it.
 */
public class SetLocaleCompleted extends CompletionEvent<SetLocale> {

    /**
     * Instantiates a new event.
     *
     * @param monitoredEvent the monitored event
     * @param channels the channels
     */
    public SetLocaleCompleted(SetLocale monitoredEvent, Channel... channels) {
        super(monitoredEvent, channels);
    }

}
