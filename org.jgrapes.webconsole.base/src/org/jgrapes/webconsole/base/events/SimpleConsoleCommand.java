/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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
 * A {@link ConsoleCommand} created directly from the JSON RPC method
 * name and parameters.
 */
public class SimpleConsoleCommand extends ConsoleCommand {

    private String method;
    private Object[] params;

    /**
     * Instantiates a new web console command.
     *
     * @param method the method
     * @param parameters the parameterss
     */
    public SimpleConsoleCommand(String method, Object... parameters) {
        this.method = method;
        this.params = Arrays.copyOf(parameters, parameters.length);
    }

    /**
     * Instantiates a new web console page command.
     *
     * @param method the method
     */
    public SimpleConsoleCommand(String method) {
        this(method, new Object[0]);
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the new parameters
     */
    public void setParameters(Object... parameters) {
        this.params = Arrays.copyOf(parameters, parameters.length);
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, method, params);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(Components.objectName(this)).append(" [method=")
            .append(method);
        if (channels() != null) {
            builder.append(", channels=")
                .append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }

}
