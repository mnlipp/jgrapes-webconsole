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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jdrupes.json.JsonArray;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.RenderSupport;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;

/**
 * Adds a web console component type with its global resources 
 * (JavaScript and/or CSS) to the console page. Specifying global 
 * resources result in the respective
 * `<link .../>` or `<script ...></script>` nodes
 * being added to the page's `<head>` node.
 * 
 * This in turn causes the browser to issue `GET` requests that
 * (usually) refer to the web console component's resources. These requests are
 * converted to {@link ConletResourceRequest}s by the web console and
 * sent to the web console components, which must respond to the requests.
 * 
 * The sequence of events is shown in the diagram.
 * 
 * ![WebConsole Ready Event Sequence](AddConletTypeSeq.svg)
 * 
 * See {@link ResourceRequest} for details about the processing
 * of the {@link ConletResourceRequest}.
 * 
 * A conlet's JavaScript may (and probably must) make use of
 * the functions provided by the web console page. See the 
 * <a href="../jsdoc/classes/console.html">JavaScript
 * documentation of these functions</a> for details.
 * 
 * @startuml AddConletTypeSeq.svg
 * hide footbox
 * 
 * activate Browser
 * Browser -> WebConsole: "consoleReady"
 * deactivate Browser
 * activate WebConsole
 * WebConsole -> ConletX: ConsoleReady 
 * deactivate WebConsole
 * activate ConletX
 * ConletX -> WebConsole: AddConletType 
 * deactivate ConletX
 * activate WebConsole
 * WebConsole -> Browser: "addConletType"
 * activate Browser
 * deactivate WebConsole
 * Browser -> WebConsole: "GET <conlet resource URI>"
 * activate WebConsole
 * WebConsole -> ConletX: ConletResourceRequest
 * deactivate Browser
 * activate ConletX
 * deactivate ConletX
 * 
 * @enduml
 */
public class AddConletType extends ConsoleCommand {

    private final String conletType;
    private Map<Locale, String> displayNames = Collections.emptyMap();
    private final List<URI> cssUris = new ArrayList<>();
    private final List<ScriptResource> scriptResources = new ArrayList<>();
    private List<RenderMode> renderModes;

    /**
     * Create a new event for the given web console component type.
     * 
     * @param conletType a unique id for the web console component type 
     * (usually the class name)
     */
    public AddConletType(String conletType) {
        this.conletType = conletType;
    }

    /**
     * Return the web console component type.
     * 
     * @return the web console component type
     */
    public String conletType() {
        return conletType;
    }

    /**
     * Sets the display names.
     * 
     * @param displayNames the display names
     * @return the event for easy chaining
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public AddConletType setDisplayNames(Map<Locale, String> displayNames) {
        this.displayNames = displayNames;
        return this;
    }

    /**
     * Return the display names.
     * 
     * @return the displayNames
     */
    public Map<Locale, String> displayNames() {
        return displayNames;
    }

    /**
     * Add a render mode to be offered to the user. Several modes may
     * be added. The modes {@link RenderModer#Preview} and 
     * {@link RenderModer#View} usually cause the conlet type to be added 
     * to a menu. If a conlet type's only mode is
     * {@link RenderModer#Component}, it shouldn't be offered to the user
     * at all.  
     *
     * @param mode the mode
     * @return the event for easy chaining
     */
    public AddConletType addRenderMode(RenderMode mode) {
        if (renderModes == null) {
            renderModes = new ArrayList<>();
        }
        renderModes.add(mode);
        return this;
    }

    /**
     * Return the render modes.
     * 
     * @return the result
     */
    public List<RenderMode> renderModes() {
        if (renderModes == null) {
            return Collections.emptyList();
        }
        return renderModes;
    }

    /**
     * Add a script resource to be requested by the browser.
     * 
     * @param scriptResource the script resource
     * @return the event for easy chaining
     */
    public AddConletType addScript(ScriptResource scriptResource) {
        scriptResources.add(scriptResource);
        return this;
    }

    /**
     * Add the URI of a CSS resource that is to be added to the
     * header section of the web console page.
     *
     * @param renderSupport the render support for mapping the `uri`
     * @param uri the URI
     * @return the event for easy chaining
     */
    public AddConletType addCss(RenderSupport renderSupport, URI uri) {
        cssUris.add(renderSupport.conletResource(conletType(), uri));
        return this;
    }

    /**
     * Return all script resources.
     * 
     * @return the result
     */
    public ScriptResource[] scriptResources() {
        return scriptResources.toArray(new ScriptResource[0]);
    }

    /**
     * Return all CSS URIs.
     * 
     * @return the result
     */
    public URI[] cssUris() {
        return cssUris.toArray(new URI[0]);
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        JsonArray strArray = JsonArray.create();
        for (ScriptResource scriptResource : scriptResources()) {
            strArray.append(scriptResource.toJsonValue());
        }
        toJson(writer, "addConletType", conletType(),
            displayNames().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLanguageTag(),
                    e -> e.getValue())),
            Arrays.stream(cssUris()).map(
                uri -> uri.toString()).toArray(String[]::new),
            strArray, renderModes().stream().map(RenderMode::name)
                .toArray(size -> new String[size]));
    }
}
