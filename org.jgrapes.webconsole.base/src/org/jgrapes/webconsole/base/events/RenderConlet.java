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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.jgrapes.webconsole.base.Conlet.RenderMode;

/**
 * A base class for web console rendering. Sent to the web console page for 
 * adding or updating a complete web console component representation. 
 * This class maintains all required information except the actual content
 * (the HTML) which must be provided by the derived classes.
 * 
 * The HTML provided is searched for attributes `data-jgwc-on-load`
 * and `data-jgwc-on-unload` which must have as value the name of a 
 * function. When the HTML has been loaded or unloaded (i.e. added to
 * the DOM or removed from the DOM), the respective functions are 
 * invoked with the element containing the attribute as their first 
 * parameter. A second boolean parameter is `true` if the on-load 
 * function is called due to an update of an already existing container.
 * 
 * The HTML elements of edit dialogs ({@link RenderMode#Edit})
 * can have an additional attribute `data-jgwc-on-apply`
 * which must have as its value the name of a function. This
 * function is invoked when changes made in the form must be
 * applied (e.g. before the dialog is closed).
 */
public abstract class RenderConlet extends ConsoleCommand {

    private static final Set<RenderMode> DEFAULT_SUPPORTED
        = Collections.unmodifiableSet(RenderMode.asSet(RenderMode.Preview));

    private final Class<?> conletClass;
    private final String conletId;
    private Set<RenderMode> renderAs = RenderMode.asSet(RenderMode.Preview);
    private Set<RenderMode> supportedModes = DEFAULT_SUPPORTED;

    /**
     * Creates a new event.
     * 
     * @param conletClass the web console component class
     * @param conletId the id of the web console component
     */
    public RenderConlet(Class<?> conletClass, String conletId) {
        this.conletClass = conletClass;
        this.conletId = conletId;
    }

    /**
     * Returns the web console component class as specified on creation.
     *
     * @return the class
     */
    public Class<?> conletClass() {
        return conletClass;
    }

    /**
     * Returns the web console component id as specified on creation.
     * 
     * @return the web console component id
     */
    public String conletId() {
        return conletId;
    }

    /**
     * Set the render mode. The default value is {@link RenderMode#Preview}.
     * 
     * @param renderMode the render mode to set
     * @return the event for easy chaining
     */
    public RenderConlet setRenderAs(RenderMode renderMode) {
        this.renderAs = RenderMode.asSet(renderMode);
        return this;
    }

    /**
     * Set the render mode (including modifier).
     * 
     * @param renderAs the render mode to use
     * @return the event for easy chaining
     */
    public RenderConlet setRenderAs(Set<RenderMode> renderAs) {
        this.renderAs = new HashSet<>(renderAs);
        return this;
    }

    /**
     * Returns the render mode.
     * 
     * @return the render mode
     */
    public Set<RenderMode> renderAs() {
        return Collections.unmodifiableSet(renderAs);
    }

    /**
     * Set the supported render modes. The default value is 
     * {@link RenderMode#Preview}.
     * 
     * @param supportedModes the supported render modes to set
     * @return the event for easy chaining
     */
    public RenderConlet setSupportedModes(Set<RenderMode> supportedModes) {
        this.supportedModes = supportedModes;
        return this;
    }

    /**
     * Add the given render mode to the supported render modes.
     * 
     * @param supportedMode the supported render modes to add
     * @return the event for easy chaining
     */
    public RenderConlet addSupportedMode(RenderMode supportedMode) {
        if (supportedModes == DEFAULT_SUPPORTED) { // NOPMD, check identity
            supportedModes = new HashSet<>(DEFAULT_SUPPORTED);
        }
        supportedModes.add(supportedMode);
        return this;
    }

    /**
     * Returns the supported modes.
     * 
     * @return the supported modes
     */
    public Set<RenderMode> supportedRenderModes() {
        return supportedModes;
    }

    /**
     * Provides the HTML that displays the web console component 
     * on the page.
     * 
     * @return the HTML
     */
    public abstract Future<String> content();

    /**
     * Writes the JSON notification to the given writer.
     *
     * @param writer the writer
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    @Override
    public void toJson(Writer writer)
            throws InterruptedException, IOException {
        try {
            toJson(writer, "updateConlet", conletId(),
                renderAs().stream().map(RenderMode::name)
                    .toArray(size -> new String[size]),
                supportedRenderModes().stream().map(RenderMode::name)
                    .toArray(size -> new String[size]),
                content().get());
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
