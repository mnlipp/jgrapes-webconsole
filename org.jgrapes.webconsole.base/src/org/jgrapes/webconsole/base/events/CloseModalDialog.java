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
 * Causes previously opened modal dialog to be closed.
 */
public class CloseModalDialog extends ConsoleCommand {

    private final String conletType;
    private final String conletId;

    /**
     * Creates a new event.
     *
     * @param conletType the conlet type
     * @param conletId the conlet id
     */
    public CloseModalDialog(String conletType, String conletId) {
        this.conletType = conletType;
        this.conletId = conletId;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "closeModalDialog", conletType, conletId);
    }

}
