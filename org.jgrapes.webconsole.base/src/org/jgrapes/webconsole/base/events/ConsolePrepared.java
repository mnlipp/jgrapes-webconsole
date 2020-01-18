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

import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;

/**
 * This event is the completion event for the {@link ConsoleReady}
 * event. A portal policy component should handle this event 
 * by sending the last known layout to the portal 
 * and triggering the rendering of the portlets
 * that are to be displayed in that layout.
 */
public class ConsolePrepared extends CompletionEvent<ConsoleReady> {

    /**
     * Instantiates a new event.
     *
     * @param monitoredEvent the monitored event
     * @param channels the channels
     */
    public ConsolePrepared(ConsoleReady monitoredEvent, Channel... channels) {
        super(monitoredEvent, channels);
        new ConsoleConfigured(this, channels);
    }
}
