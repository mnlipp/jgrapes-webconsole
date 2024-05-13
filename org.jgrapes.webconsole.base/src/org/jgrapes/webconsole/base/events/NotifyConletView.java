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
import java.util.Arrays;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;

/**
 * A notification (as defined by the JSON RPC specification) to be sent to
 * the web console component view (the browser).
 */
@SuppressWarnings("PMD.DataClass")
public class NotifyConletView extends ConsoleCommand {

    private final String conletType;
    private final String conletId;
    private final String method;
    private final Object[] params;

    /**
     * Creates a new event.
     *  
     * @param conletType the web console component type (used by the console 
     * core JS to look up the available functions, see {@link AddConletType})
     * @param conletId the web console component (view) instance that the 
     * notification is directed at
     * @param method the method (function) to be executed, must
     * have been registered by handling {@link AddConletType}
     * @param params the parameters
     */
    public NotifyConletView(String conletType,
            String conletId, String method, Object... params) {
        this.conletType = conletType;
        this.conletId = conletId;
        this.method = method;
        this.params = Arrays.copyOf(params, params.length);
    }

    /**
     * Returns the web console component class.
     * 
     * @return the web console component class
     */
    public String conletType() {
        return conletType;
    }

    /**
     * Returns the web console component id.
     * 
     * @return the web console component id
     */
    public String conletId() {
        return conletId;
    }

    /**
     * Returns the method to be executed.
     * 
     * @return the method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the parameters.
     * 
     * @return the parameters
     */
    public Object[] params() {
        return Arrays.copyOf(params, params.length);
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "notifyConletView", conletType(), conletId(),
            method(), params());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(40);
        builder.append(Components.objectName(this))
            .append(" [conletId=").append(conletId)
            .append(", method=").append(method);
        if (channels() != null) {
            builder.append(", channels=").append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }
}
