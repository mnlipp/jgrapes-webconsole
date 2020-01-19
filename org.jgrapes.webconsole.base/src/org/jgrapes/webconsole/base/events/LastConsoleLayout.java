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
import java.util.List;

import org.jdrupes.json.JsonObject;

/**
 * Sent by the server to the browser in response to {@link ConsolePrepared} 
 * (see this event's description for details). The provided information
 * enables the portal to restore web console components to their previous 
 * positions.
 */
public class LastConsoleLayout extends ConsoleCommand {

    private final List<String> previewLayout;
    private final List<String> tabsLayout;
    private final JsonObject xtraInfo;

    /**
     * @param previewLayout
     * @param tabsLayout
     */
    public LastConsoleLayout(List<String> previewLayout,
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
     * @return the extra information
     */
    public JsonObject xtraInfo() {
        return xtraInfo;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "lastConsoleLayout", previewLayout(), tabsLayout(),
            xtraInfo());
    }

}
