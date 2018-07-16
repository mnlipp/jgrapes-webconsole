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

package org.jgrapes.portal.jqueryui;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

import org.jgrapes.core.Channel;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.Session;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.base.Portal;
import org.jgrapes.portal.base.ResourceNotFoundException;
import org.jgrapes.portal.base.ThemeProvider;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortalWeblet;
import org.jgrapes.portal.jqueryui.JQueryUiWeblet;
import org.jgrapes.portal.jqueryui.themes.base.Provider;

/**
 * Provides resources using {@link Request}/{@link Response}
 * events. Some resource requests (page resource, portlet resource)
 * are forwarded via the {@link Portal} component to the portlets.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public class JQueryUiWeblet extends FreeMarkerPortalWeblet {

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
     * @param portalChannel the portal channel
     * @param portalPrefix the portal prefix
     */
    public JQueryUiWeblet(Channel webletChannel, Channel portalChannel,
            URI portalPrefix) {
        super(webletChannel, portalChannel, portalPrefix);
        baseTheme = new Provider();
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
    protected Map<String, Object> createPortalBaseModel() {
        Map<String, Object> model = super.createPortalBaseModel();
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
     * @return the portal fo reasy chaining
     */
    public JQueryUiWeblet setFallbackResourceSupplier(
            BiFunction<ThemeProvider, String, URL> supplier) {
        fallbackResourceSupplier = supplier;
        return this;
    }

    @Override
    protected void renderPortal(GetRequest event, IOSubchannel channel,
            UUID portalSessionId) throws IOException, InterruptedException {
        // Reloading themes on every reload allows themes
        // to be added dynamically. Note that we must load again
        // (not reload) in order for this to work in an OSGi environment.
        themeLoader = null;
        super.renderPortal(event, channel, portalSessionId);
    }

    @Override
    protected void providePortalResource(GetRequest event,
            String requestPath, IOSubchannel channel) {
        String[] requestParts = ResourcePattern.split(requestPath, 1);
        if (requestParts.length == 2 && requestParts[0].equals("theme")) {
            sendThemeResource(event, channel, requestParts[1]);
            return;
        }
        super.providePortalResource(event, requestPath, channel);
    }

    private void sendThemeResource(GetRequest event, IOSubchannel channel,
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

//    private void requestPageResource(GetRequest event, IOSubchannel channel,
//            String resource) throws InterruptedException {
//        // Send events to providers on portal's channel
//        PageResourceRequest pageResourceRequest = new PageResourceRequest(
//            uriFromPath(resource),
//            event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
//                Converters.DATE_TIME).orElse(null),
//            event.httpRequest(), channel, renderSupport());
//        // Make session available (associate with event, this is not
//        // a websocket request).
//        event.associated(Session.class).ifPresent(
//            session -> pageResourceRequest.setAssociated(Session.class,
//                session));
//        event.setResult(true);
//        event.stop();
//        fire(pageResourceRequest, portalChannel(channel));
//    }
//
//    private void requestPortletResource(GetRequest event, IOSubchannel channel,
//            URI resource) throws InterruptedException {
//        String resPath = resource.getPath();
//        int sep = resPath.indexOf('/');
//        // Send events to portlets on portal's channel
//        PortletResourceRequest portletRequest = new PortletResourceRequest(
//            resPath.substring(0, sep),
//            uriFromPath(resPath.substring(sep + 1)),
//            event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
//                Converters.DATE_TIME).orElse(null),
//            event.httpRequest(), channel, renderSupport());
//        // Make session available (associate with event, this is not
//        // a websocket request).
//        event.associated(Session.class).ifPresent(
//            session -> portletRequest.setAssociated(Session.class, session));
//        event.setResult(true);
//        event.stop();
//        fire(portletRequest, portalChannel(channel));
//    }
//
//    /**
//     * The portal channel for getting resources. Resource providers
//     * respond on the same event pipeline as they receive, because
//     * handling is just a mapping to {@link ResourceRequestCompleted}.
//     *
//     * @param channel the channel
//     * @return the IO subchannel
//     */
//    private IOSubchannel portalChannel(IOSubchannel channel) {
//        @SuppressWarnings("unchecked")
//        Optional<LinkedIOSubchannel> portalChannel
//            = (Optional<LinkedIOSubchannel>) LinkedIOSubchannel
//                .downstreamChannel(portal, channel);
//        return portalChannel.orElseGet(
//            () -> new PortalResourceChannel(
//                portal, channel, activeEventPipeline()));
//    }
//
//    /**
//     * Handles the {@link ResourceRequestCompleted} event.
//     *
//     * @param event the event
//     * @param channel the channel
//     * @throws IOException Signals that an I/O exception has occurred.
//     * @throws InterruptedException the interrupted exception
//     */
//    @Handler(channels = PortalChannel.class)
//    public void onResourceRequestCompleted(
//            ResourceRequestCompleted event, PortalResourceChannel channel)
//            throws IOException, InterruptedException {
//        event.stop();
//        if (event.event().get() == null) {
//            ResponseCreationSupport.sendResponse(event.event().httpRequest(),
//                event.event().httpChannel(), HttpStatus.NOT_FOUND);
//            return;
//        }
//        event.event().get().process();
//    }
//
//    private void handleSessionRequest(
//            GetRequest event, IOSubchannel channel, String portalSessionId)
//            throws InterruptedException, IOException, ParseException {
//        // Must be WebSocket request.
//        if (!event.httpRequest().findField(
//            HttpField.UPGRADE, Converters.STRING_LIST)
//            .map(fld -> fld.value().containsIgnoreCase("websocket"))
//            .orElse(false)) {
//            return;
//        }
//
//        // Can only connect to sessions that have been prepared
//        // by loading the portal. (Prevents using a newly created
//        // browser session from being (re-)connected to after a
//        // long disconnect or restart and, of course, CSF).
//        final Session browserSession = event.associated(Session.class).get();
//        @SuppressWarnings("unchecked")
//        Map<URI, UUID> knownIds
//            = (Map<URI, UUID>) browserSession.computeIfAbsent(
//                PORTAL_SESSION_IDS,
//                newKey -> new ConcurrentHashMap<URI, UUID>());
//        if (!UUID.fromString(portalSessionId) // NOPMD, note negation
//            .equals(knownIds.get(portal.prefix()))) {
//            channel.setAssociated(this, new String[2]);
//        } else {
//            channel.setAssociated(this, new String[] {
//                portalSessionId,
//                Optional.ofNullable(event.httpRequest().queryData()
//                    .get("was")).map(vals -> vals.get(0)).orElse(null)
//            });
//        }
//        channel.respond(new ProtocolSwitchAccepted(event, "websocket"));
//        event.stop();
//    }
//
////    /**
////     * Called when the connection has been upgraded.
////     *
////     * @param event the event
////     * @param wsChannel the ws channel
////     * @throws IOException Signals that an I/O exception has occurred.
////     */
////    @Handler
////    public void onUpgraded(Upgraded event, IOSubchannel wsChannel)
////            throws IOException {
////        Optional<String[]> passedIn
////            = wsChannel.associated(this, String[].class);
////        if (!passedIn.isPresent()) {
////            return;
////        }
////
////        // Check if reload required
////        String[] portalSessionIds = passedIn.get();
////        if (portalSessionIds[0] == null) {
////            @SuppressWarnings("resource")
////            CharBufferWriter out = new CharBufferWriter(wsChannel,
////                wsChannel.responsePipeline()).suppressClose();
////            new SimplePortalCommand("reload").toJson(out);
////            out.close();
////            event.stop();
////            return;
////        }
////
////        // Get portal session
////        final Session browserSession
////            = wsChannel.associated(Session.class).get();
////        // Reuse old portal session if still available
////        PortalSession portalSession = Optional.ofNullable(portalSessionIds[1])
////            .flatMap(opsId -> PortalSession.lookup(opsId))
////            .map(session -> session.replaceId(portalSessionIds[0]))
////            .orElse(PortalSession.lookupOrCreate(portalSessionIds[0],
////                portal, psNetworkTimeout))
////            .setUpstreamChannel(wsChannel)
////            .setSession(browserSession);
////        wsChannel.setAssociated(PortalSession.class, portalSession);
////        // Channel now used as JSON input
////        wsChannel.setAssociated(this, new WebSocketInputReader(
////            event.processedBy().get(), portalSession));
////        // From now on, only portalSession.respond may be used to send on the
////        // upstream channel.
////        portalSession.upstreamChannel().responsePipeline()
////            .restrictEventSource(portalSession.responsePipeline());
////    }
//
//    /**
//     * Handles network input (JSON data).
//     *
//     * @param event the event
//     * @param wsChannel the ws channel
//     * @throws IOException Signals that an I/O exception has occurred.
//     */
//    @Handler
//    public void onInput(Input<CharBuffer> event, IOSubchannel wsChannel)
//            throws IOException {
//        Optional<WebSocketInputReader> optWsInputReader
//            = wsChannel.associated(this, WebSocketInputReader.class);
//        if (optWsInputReader.isPresent()) {
//            optWsInputReader.get().write(event.buffer().backingBuffer());
//        }
//    }
//
////    /**
////     * Handles the closed event from the web socket.
////     *
////     * @param event the event
////     * @param wsChannel the WebSocket channel
////     * @throws IOException Signals that an I/O exception has occurred.
////     */
////    @Handler
////    public void onClosed(
////            Closed event, IOSubchannel wsChannel) throws IOException {
////        Optional<WebSocketInputReader> optWsInputReader
////            = wsChannel.associated(this, WebSocketInputReader.class);
////        if (optWsInputReader.isPresent()) {
////            wsChannel.setAssociated(this, null);
////            optWsInputReader.get().close();
////        }
////        wsChannel.associated(PortalSession.class).ifPresent(session -> {
////            // Restore channel to normal mode, see onPortalReady
////            session.responsePipeline().restrictEventSource(null);
////            session.disconnected();
////        });
////    }
//
//    /**
//     * Handles the {@link PortalReady} event.
//     *
//     * @param event the event
//     * @param portalSession the portal session
//     */
//    @Handler(channels = PortalChannel.class)
//    public void onPortalReady(PortalReady event, PortalSession portalSession) {
//        String principal = Utils.userFromSession(portalSession.browserSession())
//            .map(UserPrincipal::toString).orElse("");
//        KeyValueStoreQuery query = new KeyValueStoreQuery(
//            "/" + principal + "/themeProvider", portalSession);
//        fire(query, portalSession);
//    }
//
//    /**
//     * Handles the data retrieved from storage.
//     *
//     * @param event the event
//     * @param channel the channel
//     * @throws JsonDecodeException the json decode exception
//     */
//    @Handler(channels = PortalChannel.class)
//    public void onKeyValueStoreData(
//            KeyValueStoreData event, PortalSession channel)
//            throws JsonDecodeException {
//        Session session = channel.browserSession();
//        String principal = Utils.userFromSession(session)
//            .map(UserPrincipal::toString).orElse("");
//        if (!event.event().query().equals("/" + principal + "/themeProvider")) {
//            return;
//        }
//        if (!event.data().values().iterator().hasNext()) {
//            return;
//        }
//        String requestedThemeId = event.data().values().iterator().next();
//        ThemeProvider themeProvider = Optional.ofNullable(
//            session.get("themeProvider")).flatMap(
//                themeId -> StreamSupport
//                    .stream(themeLoader().spliterator(), false)
//                    .filter(thi -> thi.themeId().equals(themeId)).findFirst())
//            .orElse(baseTheme);
//        if (!themeProvider.themeId().equals(requestedThemeId)) {
//            fire(new SetTheme(requestedThemeId), channel);
//        }
//    }
//
//    /**
//     * Handles a change of theme.
//     *
//     * @param event the event
//     * @param channel the channel
//     * @throws InterruptedException the interrupted exception
//     * @throws IOException Signals that an I/O exception has occurred.
//     */
//    @Handler(channels = PortalChannel.class)
//    public void onSetTheme(SetTheme event, PortalSession channel)
//            throws InterruptedException, IOException {
//        ThemeProvider themeProvider = StreamSupport
//            .stream(themeLoader().spliterator(), false)
//            .filter(thi -> thi.themeId().equals(event.theme())).findFirst()
//            .orElse(baseTheme);
//        channel.browserSession().put("themeProvider", themeProvider.themeId());
//        channel.respond(new KeyValueStoreUpdate().update(
//            "/" + Utils.userFromSession(channel.browserSession())
//                .map(UserPrincipal::toString).orElse("")
//                + "/themeProvider",
//            themeProvider.themeId())).get();
//        channel.respond(new SimplePortalCommand("reload"));
//    }
//
//    /**
//     * Sends a command to the portal.
//     *
//     * @param event the event
//     * @param channel the channel
//     * @throws InterruptedException the interrupted exception
//     * @throws IOException Signals that an I/O exception has occurred.
//     */
//    @Handler(channels = PortalChannel.class, priority = -1000)
//    public void onPortalCommand(
//            PortalCommand event, PortalSession channel)
//            throws InterruptedException, IOException {
//        IOSubchannel upstream = channel.upstreamChannel();
//        @SuppressWarnings("resource")
//        CharBufferWriter out = new CharBufferWriter(upstream,
//            upstream.responsePipeline()).suppressClose();
//        event.toJson(out);
//    }
//
//    /**
//     * Holds the information about the selected language.
//     */
//    public static class LanguageInfo {
//        private final Locale locale;
//
//        /**
//         * Instantiates a new language info.
//         *
//         * @param locale the locale
//         */
//        public LanguageInfo(Locale locale) {
//            this.locale = locale;
//        }
//
//        /**
//         * Gets the locale.
//         *
//         * @return the locale
//         */
//        public Locale getLocale() {
//            return locale;
//        }
//
//        /**
//         * Gets the label.
//         *
//         * @return the label
//         */
//        public String getLabel() {
//            String str = locale.getDisplayName(locale);
//            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
//        }
//    }
//
//    /**
//     * Holds the information about a theme.
//     */
//    public static class ThemeInfo implements Comparable<ThemeInfo> {
//        private final String id;
//        private final String name;
//
//        /**
//         * Instantiates a new theme info.
//         *
//         * @param id the id
//         * @param name the name
//         */
//        public ThemeInfo(String id, String name) {
//            super();
//            this.id = id;
//            this.name = name;
//        }
//
//        /**
//         * Returns the id.
//         *
//         * @return the id
//         */
//        @SuppressWarnings("PMD.ShortMethodName")
//        public String id() {
//            return id;
//        }
//
//        /**
//         * Returns the name.
//         *
//         * @return the name
//         */
//        public String name() {
//            return name;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see java.lang.Comparable#compareTo(java.lang.Object)
//         */
//        @Override
//        public int compareTo(ThemeInfo other) {
//            return name().compareToIgnoreCase(other.name());
//        }
//    }
//
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
//     * {@link PortletResourceRequest}s to the portlets (via the
//     * portal).
//     */
//    public class PortalResourceChannel extends LinkedIOSubchannel {
//
//        /**
//         * Instantiates a new portal resource channel.
//         *
//         * @param hub the hub
//         * @param upstreamChannel the upstream channel
//         * @param responsePipeline the response pipeline
//         */
//        public PortalResourceChannel(Manager hub,
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
//        /*
//         * (non-Javadoc)
//         * 
//         * @see
//         * org.jgrapes.portal.RenderSupport#portletResource(java.lang.String,
//         * java.net.URI)
//         */
//        @Override
//        public URI portletResource(String portletType, URI uri) {
//            return portal.prefix().resolve(uriFromPath(
//                "portlet-resource/" + portletType + "/")).resolve(uri);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see org.jgrapes.portal.RenderSupport#pageResource(java.net.URI)
//         */
//        @Override
//        public URI pageResource(URI uri) {
//            return portal.prefix().resolve(uriFromPath(
//                "page-resource/")).resolve(uri);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see org.jgrapes.portal.RenderSupport#useMinifiedResources()
//         */
//        @Override
//        public boolean useMinifiedResources() {
//            return useMinifiedResources;
//        }
//
//    }
//
}
