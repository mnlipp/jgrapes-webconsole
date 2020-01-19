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
 * A notification (as defined by the JSON RPC specification) to be sent to
 * the web console component view (the browser).
 */
public class DeleteConlet extends ConsoleCommand {

    private final String conletId;

    /**
     * Creates a new event.
     *  
     * @param conletId the web console component (view) that should be deleted
     */
    public DeleteConlet(String conletId) {
        this.conletId = conletId;
    }

    /**
     * Returns the web console component id.
     * 
     * @return the web console component id
     */
    public String conletId() {
        return conletId;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "deleteConlet", conletId());
    }

}
