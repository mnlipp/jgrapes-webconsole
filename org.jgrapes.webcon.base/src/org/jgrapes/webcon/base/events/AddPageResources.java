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

package org.jgrapes.webcon.base.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jdrupes.json.JsonArray;
import org.jdrupes.json.JsonObject;
import org.jgrapes.webcon.base.PageResourceProvider;
import org.jgrapes.webcon.base.freemarker.FreeMarkerConsoleWeblet;

/**
 * Adds `<link .../>`, `<style>...</style>` or `<script ...></script>` nodes
 * to the portal's `<head>` node on behalf of components that provide
 * such resources.
 * 
 * Adding resource references causes the browser to issue `GET` request that
 * (usually) refer to resources that are then provided by the component
 * that created the {@link AddPageResources} event.
 * 
 * The sequence of events is shown in the diagram.
 * 
 * ![WebConsole Ready Event Sequence](AddToHead.svg)
 * 
 * See {@link ResourceRequest} for details about the processing
 * of the {@link PageResourceRequest}.
 * 
 * The `GET` request may also, of course, refer to a resource from 
 * another server and thus not result in a {@link PageResourceRequest}.
 * 
 * Adding a `<script src=...></script>` node to a document's `<head>` 
 * causes the referenced JavaScript to be loaded asynchronously. This
 * can cause problems if a dynamically added library relies on another 
 * library to be available. Script resources are therefore specified using
 * the {@link ScriptResource} class, which allows to specify loading 
 * dependencies between resources. The code in the browser delays the 
 * addition of a `<script>` node until all other script resources that 
 * it depends on are loaded.
 * 
 * Some libraries provided as page resources may already be required by the
 * JavaScript portal code (especially by the resource manager that handles
 * the delayed loading). They can therefore not be loaded by this
 * mechanism, which depends on the portal code. Such page resources
 * may be "pre-loaded" by adding the appropriate `script` element to the
 * initial portal page. In order to make the pre-loading known to the
 * resource manager, the `script` elements must carry an attribute
 * `data-jgp-provides` with a comma separated list of JavaScript resource
 * names provided by loading the script resource. The name(s) must match
 * the name(s) used in the {@link AddPageResources} request generated
 * by the {@link PageResourceProvider} for the pre-loaded resource(s).
 * Here's an example (for a portal using the {@link FreeMarkerConsoleWeblet}
 * to generate the initial portal page):
 * ```html
 * {@code <}script data-jgp-provides="jquery"
 *   src="${renderSupport.pageResource('jquery/jquery' + minifiedExtension + '.js')}"{@code >}
 * {@code <}/script{@code >};
 * ``` 
 * 
 * @startuml AddToHead.svg
 * hide footbox
 * 
 * activate Browser
 * Browser -> WebConsole: "portalReady"
 * deactivate Browser
 * activate WebConsole
 * WebConsole -> PageResourceProvider: ConsoleReady 
 * activate PageResourceProvider
 * PageResourceProvider -> WebConsole: AddPageResources
 * deactivate PageResourceProvider
 * WebConsole -> Browser: "addPageResource"
 * deactivate WebConsole
 * activate Browser
 * deactivate WebConsole
 * Browser -> WebConsole: "GET <page resource URI>"
 * activate WebConsole
 * WebConsole -> PageResourceProvider: PageResourceRequest
 * deactivate WebConsole
 * activate PageResourceProvider
 * deactivate PageResourceProvider
 * @enduml
 */
public class AddPageResources extends ConsoleCommand {

    private final List<ScriptResource> scriptResources = new ArrayList<>();
    private final List<URI> cssUris = new ArrayList<>();
    private String cssSource;

    /**
     * Add the URI of a JavaScript resource that is to be added to the
     * header section of the portal page.
     * 
     * @param scriptResource the resource to add
     * @return the event for easy chaining
     */
    public AddPageResources addScriptResource(ScriptResource scriptResource) {
        scriptResources.add(scriptResource);
        return this;
    }

    /**
     * Return all script URIs
     * 
     * @return the result
     */
    public ScriptResource[] scriptResources() {
        return scriptResources.toArray(new ScriptResource[0]);
    }

    /**
     * Add the URI of a CSS resource that is to be added to the
     * header section of the portal page.
     * 
     * @param uri the URI
     * @return the event for easy chaining
     */
    public AddPageResources addCss(URI uri) {
        cssUris.add(uri);
        return this;
    }

    /**
     * Return all CSS URIs.
     * 
     * @return the result
     */
    public URI[] cssUris() {
        return cssUris.toArray(new URI[0]);
    }

    /**
     * @return the cssSource
     */
    public String cssSource() {
        return cssSource;
    }

    /**
     * @param cssSource the cssSource to set
     */
    public AddPageResources setCssSource(String cssSource) {
        this.cssSource = cssSource;
        return this;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        JsonArray scripts = JsonArray.create();
        for (ScriptResource scriptResource : scriptResources()) {
            scripts.append(scriptResource.toJsonValue());
        }
        toJson(writer, "addPageResources", Arrays.stream(cssUris()).map(
            uri -> uri.toString()).toArray(String[]::new),
            cssSource(), scripts);
    }

    /**
     * Represents a script resource that is to be loaded or evaluated
     * by the browser. Note that a single instance can either be used
     * for a URI or inline JavaScript, not for both.
     */
    public static class ScriptResource {
        private static final String[] EMPTY_ARRAY = new String[0];

        private URI scriptUri;
        private String scriptId;
        private String scriptType;
        private String scriptSource;
        private String[] provides = EMPTY_ARRAY;
        private String[] requires = EMPTY_ARRAY;

        /**
         * @return the scriptUri to be loaded
         */
        public URI scriptUri() {
            return scriptUri;
        }

        /**
         * Sets the scriptUri to to be loaded, clears the `scriptSource`
         * attribute.
         * 
         * @param scriptUri the scriptUri to to be loaded
         * @return this object for easy chaining
         */
        public ScriptResource setScriptUri(URI scriptUri) {
            this.scriptUri = scriptUri;
            return this;
        }

        /**
         * Gets the script type (defaults to no type).
         *
         * @return the script type
         */
        public String getScriptType() {
            return scriptType;
        }

        /**
         * Sets the script type.
         *
         * @param scriptType the new script type
         * @return the script resource
         */
        public ScriptResource setScriptType(String scriptType) {
            this.scriptType = scriptType;
            return this;
        }

        /**
         * Gets the script id (defaults to no id).
         *
         * @return the script type
         */
        public String getScriptId() {
            return scriptId;
        }

        /**
         * Sets the script id.
         *
         * @param scriptId the script id
         * @return the script resource
         */
        public ScriptResource setScriptId(String scriptId) {
            this.scriptId = scriptId;
            return this;
        }

        /**
         * @return the script source
         */
        public String scriptSource() {
            return scriptSource;
        }

        /**
         * Sets the script source to evaluate. Clears the
         * `scriptUri` attribute.
         * 
         * @param scriptSource the scriptSource to set
         * @return this object for easy chaining
         */
        public ScriptResource setScriptSource(String scriptSource) {
            this.scriptSource = scriptSource;
            scriptUri = null;
            return this;
        }

        /**
         * Loads the script source to evaluate. Clears the
         * `scriptUri` attribute. Closes the reader.
         *
         * @param in the input stream
         * @return this object for easy chaining
         * @throws IOException 
         */
        public ScriptResource loadScriptSource(Reader in) throws IOException {
            try (BufferedReader buffered = new BufferedReader(in)) {
                this.scriptSource
                    = buffered.lines().collect(Collectors.joining("\r\n"));
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
            scriptUri = null;
            return this;
        }

        /**
         * Returns the list of JavaScript features that this
         * script resource provides.
         * 
         * @return the list of features
         */
        public String[] provides() {
            return Arrays.copyOf(provides, provides.length);
        }

        /**
         * Sets the list of JavaScript features that this
         * script resource provides. For commonly available
         * JavaScript libraries, it is recommended to use
         * their home page URL (without the protocol part) as
         * feature name. 
         * 
         * @param provides the list of features
         * @return this object for easy chaining
         */
        public ScriptResource setProvides(String... provides) {
            this.provides = Arrays.copyOf(provides, provides.length);
            return this;
        }

        /**
         * Returns the list of JavaScript features that this
         * script resource requires.
         * 
         * @return the list of features
         */
        public String[] requires() {
            return Arrays.copyOf(requires, requires.length);
        }

        /**
         * Sets the list of JavaScript features that this
         * script resource requires.
         * 
         * @param requires the list of features
         * @return this object for easy chaining
         */
        public ScriptResource setRequires(String... requires) {
            this.requires = Arrays.copyOf(requires, requires.length);
            return this;
        }

        /**
         * Provides the JSON representation of the information.
         *
         * @return the json object
         */
        public JsonObject toJsonValue() {
            JsonObject obj = JsonObject.create();
            if (scriptUri != null) {
                obj.setField("uri", scriptUri.toString());
            }
            if (scriptId != null) {
                obj.setField("id", scriptId);
            }
            if (scriptType != null) {
                obj.setField("type", scriptType);
            }
            if (scriptSource != null) {
                obj.setField("source", scriptSource);
            }
            JsonArray strArray = JsonArray.create();
            for (String req : requires) {
                strArray.append(req);
            }
            obj.setField("requires", strArray);
            strArray = JsonArray.create();
            for (String prov : provides) {
                strArray.append(prov);
            }
            obj.setField("provides", strArray);
            return obj;
        }
    }
}
