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

package org.jgrapes.webconsole.jqueryui;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

import org.jdrupes.json.JsonArray;
import org.jgrapes.core.Channel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.Session;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.ResourceNotFoundException;
import org.jgrapes.webconsole.base.UserPrincipal;
import org.jgrapes.webconsole.base.WebConsole;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.JsonInput;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConsoleWeblet;
import org.jgrapes.webconsole.jqueryui.events.SetTheme;
import org.jgrapes.webconsole.jqueryui.themes.base.Provider;

/**
 * Provides resources using {@link Request}/{@link Response}
 * events. Some resource requests (page resource, portlet resource)
 * are forwarded via the {@link WebConsole} component to the portlets.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public class JQueryUiWeblet extends FreeMarkerConsoleWeblet {

    private ServiceLoader<ThemeProvider> themeLoader;
    private final ThemeProvider baseTheme;
    private BiFunction<ThemeProvider, String, URL> fallbackResourceSupplier
        = (themeProvider, resource) -> {
            return null;
        };

    /**
     * Instantiates a new jQuery UI weblet.
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the web console channel
     * @param consolePrefix the web console prefix
     */
    public JQueryUiWeblet(Channel webletChannel, Channel consoleChannel,
            URI consolePrefix) {
        super(webletChannel, consoleChannel, consolePrefix);
        baseTheme = new Provider();
    }

    @Override
    public String styling() {
        return "jqueryui";
    }

    /**
     * The service loader must be created lazily, else the OSGi
     * service mediator doesn't work properly.
     * 
     * @return
     */
    private ServiceLoader<ThemeProvider> themeLoader() {
        if (themeLoader != null) {
            return themeLoader;
        }
        return themeLoader = ServiceLoader.load(ThemeProvider.class);
    }

    @Override
    protected Map<String, Object> createConsoleBaseModel() {
        Map<String, Object> model = super.createConsoleBaseModel();
        model.put("themeInfos",
            StreamSupport.stream(themeLoader().spliterator(), false)
                .map(thi -> new ThemeInfo(thi.themeId(), thi.themeName()))
                .sorted().toArray(size -> new ThemeInfo[size]));
        return model;
    }

    /**
     * Sets a function for obtaining a fallback resource bundle for
     * a given theme provider and locale.
     * 
     * @param supplier the function
     * @return the web console fo reasy chaining
     */
    public JQueryUiWeblet setFallbackResourceSupplier(
            BiFunction<ThemeProvider, String, URL> supplier) {
        fallbackResourceSupplier = supplier;
        return this;
    }

    @Override
    protected void renderConsole(Request.In.Get event, IOSubchannel channel,
            UUID consoleSessionId) throws IOException, InterruptedException {
        // Reloading themes on every reload allows themes
        // to be added dynamically. Note that we must load again
        // (not reload) in order for this to work in an OSGi environment.
        themeLoader = null;
        super.renderConsole(event, channel, consoleSessionId);
    }

    @Override
    protected void provideConsoleResource(Request.In.Get event,
            String requestPath, IOSubchannel channel) {
        String[] requestParts = ResourcePattern.split(requestPath, 1);
        if (requestParts.length == 2 && requestParts[0].equals("theme")) {
            sendThemeResource(event, channel, requestParts[1]);
            return;
        }
        super.provideConsoleResource(event, requestPath, channel);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void sendThemeResource(Request.In.Get event, IOSubchannel channel,
            String resource) {
        // Get resource
        ThemeProvider themeProvider = event.associated(Session.class).flatMap(
            session -> Optional.ofNullable(session.get("themeProvider"))
                .flatMap(
                    themeId -> StreamSupport
                        .stream(themeLoader().spliterator(), false)
                        .filter(thi -> thi.themeId().equals(themeId))
                        .findFirst()))
            .orElse(baseTheme);
        URL resourceUrl;
        try {
            resourceUrl = themeProvider.getResource(resource);
        } catch (ResourceNotFoundException e) {
            try {
                resourceUrl = baseTheme.getResource(resource);
            } catch (ResourceNotFoundException e1) {
                resourceUrl
                    = fallbackResourceSupplier.apply(themeProvider, resource);
                if (resourceUrl == null) {
                    return;
                }
            }
        }
        final URL resUrl = resourceUrl;
        ResponseCreationSupport.sendStaticContent(event, channel,
            path -> resUrl, null);
    }

    /**
     * Handle JSON input.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @Handler(channels = ConsoleChannel.class)
    public void onJsonInput(JsonInput event, ConsoleSession channel)
            throws InterruptedException, IOException {
        // Send events to portlets on web console's channel
        JsonArray params = event.request().params();
        switch (event.request().method()) { // NOPMD
        case "setTheme": {
            fire(new SetTheme(params.asString(0)), channel);
            break;
        }
        default:
            // Ignore unknown
            break;
        }
    }

    /**
     * Handles a change of theme.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = ConsoleChannel.class)
    public void onSetTheme(SetTheme event, ConsoleSession channel)
            throws InterruptedException, IOException {
        ThemeProvider themeProvider = StreamSupport
            .stream(themeLoader().spliterator(), false)
            .filter(thi -> thi.themeId().equals(event.theme())).findFirst()
            .orElse(baseTheme);
        channel.browserSession().put("themeProvider", themeProvider.themeId());
        channel.respond(new KeyValueStoreUpdate().update(
            "/" + WebConsoleUtils.userFromSession(channel.browserSession())
                .map(UserPrincipal::toString).orElse("")
                + "/themeProvider",
            themeProvider.themeId())).get();
        channel.respond(new SimpleConsoleCommand("reload"));
    }

    /**
     * Holds the information about a theme.
     */
    public static class ThemeInfo implements Comparable<ThemeInfo> {
        private final String id;
        private final String name;

        /**
         * Instantiates a new theme info.
         *
         * @param id the id
         * @param name the name
         */
        public ThemeInfo(String id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        /**
         * Returns the id.
         *
         * @return the id
         */
        @SuppressWarnings("PMD.ShortMethodName")
        public String id() {
            return id;
        }

        /**
         * Returns the name.
         *
         * @return the name
         */
        public String name() {
            return name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(ThemeInfo other) {
            return name().compareToIgnoreCase(other.name());
        }
    }

//    /**
//     * Create a {@link URI} from a path. This is similar to calling
//     * `new URI(null, null, path, null)` with the {@link URISyntaxException}
//     * converted to a {@link IllegalArgumentException}.
//     * 
//     * @param path the path
//     * @return the uri
//     * @throws IllegalArgumentException if the string violates 
//     * RFC 2396
//     */
//    public static URI uriFromPath(String path) throws IllegalArgumentException {
//        try {
//            return new URI(null, null, path, null);
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException(e);
//        }
//    }
//
//    /**
//     * The channel used to send {@link PageResourceRequest}s and
//     * {@link ConletResourceRequest}s to the portlets (via the
//     * web console).
//     */
//    public class ConsoleResourceChannel extends LinkedIOSubchannel {
//
//        /**
//         * Instantiates a new web console resource channel.
//         *
//         * @param hub the hub
//         * @param upstreamChannel the upstream channel
//         * @param responsePipeline the response pipeline
//         */
//        public ConsoleResourceChannel(Manager hub,
//                IOSubchannel upstreamChannel, EventPipeline responsePipeline) {
//            super(hub, hub.channel(), upstreamChannel, responsePipeline);
//        }
//    }
//
//    /**
//     * The implementation of {@link RenderSupport} used by this class.
//     */
//    private class RenderSupportImpl implements RenderSupport {
//
//        @Override
//        public URI portletResource(String portletType, URI uri) {
//            return console.prefix().resolve(uriFromPath(
//                "portlet-resource/" + portletType + "/")).resolve(uri);
//        }
//
//        @Override
//        public URI pageResource(URI uri) {
//            return console.prefix().resolve(uriFromPath(
//                "page-resource/")).resolve(uri);
//        }
//
//        @Override
//        public boolean useMinifiedResources() {
//            return useMinifiedResources;
//        }
//
//    }
//
}
