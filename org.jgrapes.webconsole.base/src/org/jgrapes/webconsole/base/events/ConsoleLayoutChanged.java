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

import java.util.List;

import org.jdrupes.json.JsonObject;
import org.jgrapes.core.Event;

/**
 * Notifies the server about a change of the web console layout. This event
 * is sent by the browser when the layout changes due to user interaction
 * (e.g. moving the preview representations) or due to notifications
 * from the server (e.g. {@link RenderConlet}).
 * 
 * The server should persist the layout and pass it back to the browser
 * in response to {@link ConsoleReady} (see this event's description
 * for details). 
 */
public class ConsoleLayoutChanged extends Event<Void> {

    private final List<String> previewLayout;
    private final List<String> tabsLayout;
    private final JsonObject xtraInfo;

    /**
     * @param previewLayout
     * @param tabsLayout
     */
    public ConsoleLayoutChanged(List<String> previewLayout,
            List<String> tabsLayout, JsonObject xtraInfo) {
        this.previewLayout = previewLayout;
        this.tabsLayout = tabsLayout;
        this.xtraInfo = xtraInfo;
    }

    /**
     * @return the previewLayout
     */
    public List<String> previewLayout() {
        return previewLayout;
    }

    /**
     * @return the tabsLayout
     */
    public List<String> tabsLayout() {
        return tabsLayout;
    }

    /**
     * The extra information.
     *
     * @return the value
     */
    public JsonObject xtraInfo() {
        return xtraInfo;
    }

}
