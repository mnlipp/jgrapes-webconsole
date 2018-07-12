/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.portal.base.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jgrapes.portal.base.Portlet.RenderMode;

/**
 * A base class for portal commands Sent to the portal page for 
 * adding or updating a complete portlet representation. This class
 * maintains all required information except the actual content
 * (the HTML) which must be provided by the derived classes.
 */
public abstract class RenderPortlet extends PortalCommand {

    private static final Set<RenderMode> DEFAULT_SUPPORTED
        = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(new RenderMode[] { RenderMode.Preview })));

    private final Class<?> portletClass;
    private final String portletId;
    private RenderMode renderMode = RenderMode.Preview;
    private Set<RenderMode> supportedModes = DEFAULT_SUPPORTED;
    private boolean foreground;

    /**
     * Creates a new event.
     * 
     * @param portletClass the portlet class
     * @param portletId the id of the portlet
     */
    public RenderPortlet(Class<?> portletClass, String portletId) {
        this.portletClass = portletClass;
        this.portletId = portletId;
    }

    /**
     * Returns the portlet class as specified on creation.
     *
     * @return the class
     */
    public Class<?> portletClass() {
        return portletClass;
    }

    /**
     * Returns the portlet id as specified on creation.
     * 
     * @return the portlet id
     */
    public String portletId() {
        return portletId;
    }

    /**
     * Set the render mode. The default value is {@link RenderMode#Preview}.
     * 
     * @param renderMode the render mode to set
     * @return the event for easy chaining
     */
    public RenderPortlet setRenderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        return this;
    }

    /**
     * Returns the render mode.
     * 
     * @return the render mode
     */
    public RenderMode renderMode() {
        return renderMode;
    }

    /**
     * Set the supported render modes. The default value is 
     * {@link RenderMode#Preview}.
     * 
     * @param supportedModes the supported render modes to set
     * @return the event for easy chaining
     */
    public RenderPortlet setSupportedModes(Set<RenderMode> supportedModes) {
        this.supportedModes = supportedModes;
        return this;
    }

    /**
     * Add the given render mode to the supported render modes.
     * 
     * @param supportedMode the supported render modes to add
     * @return the event for easy chaining
     */
    public RenderPortlet addSupportedMode(RenderMode supportedMode) {
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
     * Id set, the tab with the portlet is put in the foreground
     * when the portlet is rendered. The default value is `false`.
     * 
     * @param foreground if set, the portlet is put in foreground
     * @return the event for easy chaining
     */
    public RenderPortlet setForeground(boolean foreground) {
        this.foreground = foreground;
        return this;
    }

    /**
     * Indicates if portelt is to be put in foreground.
     * 
     * @return the result
     */
    public boolean isForeground() {
        return foreground;
    }

    /**
     * Provides the HTML that displays the portlet 
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
            toJson(writer, "updatePortlet", portletId(), renderMode().name(),
                supportedRenderModes().stream().map(RenderMode::name)
                    .toArray(size -> new String[size]),
                content().get(), isForeground());
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
