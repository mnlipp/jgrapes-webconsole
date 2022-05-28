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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Causes a modal dialog to be display on behalf of the conlet.
 */
public class OpenModalDialog extends ConsoleCommand {

    private final String conletType;
    private final String conletId;
    private final Future<String> content;
    private Map<String, Object> options;

    /**
     * Creates a new event. The content must be valid HTML, i.e. it
     * must start with a tag.  See the JavaScript documentation of the
     * <a href="../jsdoc/interfaces/ModalDialogOptions.html">
     * modal dialog options</a> for details.
     *
     * @param conletType the conlet type
     * @param conletId the conlet id
     * @param content the content (valid HTML)
     * @param options the options (must be serializable as JSON), see
     * the JavaScript documentation of the
     * <a href="../jsdoc/interfaces/ModalDialogOptions.html">
     * modal dialog options</a> for details.
     */
    public OpenModalDialog(String conletType, String conletId,
            Future<String> content, Map<String, Object> options) {
        this.conletType = conletType;
        this.conletId = conletId;
        this.content = content;
        this.options = options;
    }

    /**
     * Creates a new event without any options.
     * The content must be valid HTML, i.e. it
     * must start with a tag.
     *
     * @param conletType the conlet type
     * @param conletId the conlet id
     * @param content the content (valid HTML)
     */
    public OpenModalDialog(String conletType, String conletId,
            Future<String> content) {
        this(conletType, conletId, content, null);
    }

    /**
     * Adds an option to the event.
     * 
     * @param name the option name
     * @param value the option value (must be serializable as JSON)
     * @return the event for easy chaining
     */
    public OpenModalDialog addOption(String name, Object value) {
        if (options == null) {
            options = new HashMap<>();
        }
        options.put(name, value);
        return this;
    }

    /**
     * Returns the content.
     * 
     * @return the content
     */
    public Future<String> content() {
        return content;
    }

    /**
     * Return the options.
     * 
     * @return the options
     */
    public Map<String, Object> options() {
        return options == null ? Collections.emptyMap() : options;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        Map<String, Object> options = options();
        try {
            toJson(writer, "openModalDialog", conletType, conletId,
                content().get(), options);
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

}
