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

package org.jgrapes.portal.base;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.Session;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.base.events.PageResourceRequest;
import org.jgrapes.portal.base.events.PortalCommand;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.PortletResourceRequest;
import org.jgrapes.portal.base.events.ResourceRequestCompleted;
import org.jgrapes.portal.base.events.SetLocale;
import org.jgrapes.portal.base.events.SimplePortalCommand;
import org.jgrapes.util.events.KeyValueStoreQuery;

/**
 * Provides resources using {@link Request}/{@link Response}
 * events. Some resource requests (page resource, portlet resource)
 * are forwarded via the {@link Portal} component to the portlets.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public abstract class PortalWeblet extends Component {

    private static final String PORTAL_SESSION_IDS
        = PortalWeblet.class.getName() + ".portalSessionId";
    private static final String UTF_8 = "utf-8";

    private URI prefix;
    private final Portal portal;
    private ResourcePattern requestPattern;

    private final RenderSupport renderSupport = new RenderSupportImpl();
    private boolean useMinifiedResources = true;
    private long psNetworkTimeout = 45000;
    private long psRefreshInterval = 30000;
    private long psInactivityTimeout = -1;

    private List<Class<?>> portalResourceSearchSeq;
    private Function<Locale, ResourceBundle> resourceBundleSupplier;
    private final Set<Locale> supportedLocales = new HashSet<>();

    /**
     * The class used in handler annotations to represent the 
     * portal channel.
     */
    protected class PortalChannel extends ClassChannel {
    }

    /**
     * Instantiates a new portal weblet.
     *
     * @param webletChannel the weblet channel
     * @param portalChannel the portal channel
     * @param portalPrefix the portal prefix
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public PortalWeblet(Channel webletChannel, Channel portalChannel,
            URI portalPrefix) {
        this(webletChannel, new Portal(portalChannel));

        prefix = URI.create(portalPrefix.getPath().endsWith("/")
            ? portalPrefix.getPath()
            : (portalPrefix.getPath() + "/"));
        portal.setView(this);

        String portalPath = prefix.getPath();
        if (portalPath.endsWith("/")) {
            portalPath = portalPath.substring(0, portalPath.length() - 1);
        }
        portalPath = portalPath + "|**";
        try {
            requestPattern = new ResourcePattern(portalPath);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        portalResourceSearchSeq = portalHierarchy();
        updateSupportedLocales();

        RequestHandler.Evaluator.add(this, "onGet", prefix + "**");
        RequestHandler.Evaluator.add(this, "onGetRedirect",
            prefix.getPath().substring(
                0, prefix.getPath().length() - 1));
    }

    private PortalWeblet(Channel webletChannel, Portal portal) {
        super(webletChannel, ChannelReplacements.create()
            .add(PortalChannel.class, portal.channel()));
        this.portal = portal;
        attach(portal);
    }

    protected List<Class<?>> portalHierarchy() {
        List<Class<?>> result = new ArrayList<>();
        Class<?> derivative = getClass();
        while (true) {
            result.add(derivative);
            if (derivative.equals(PortalWeblet.class)) {
                break;
            }
            derivative = derivative.getSuperclass();
        }
        return result;
    }

//    private Function<Locale, ResourceBundle> resourceBundleSupplier
//        = locale -> {
//            ResourceBundle last = null;
//            List<Class<?>> clses = portalHierarchy();
//            Collections.reverse(clses);
//            for (Class<?> cls : clses) {
//                last = new LinkedResourceBundle(ResourceBundle.getBundle(
//                    cls.getPackage().getName() + ".l10n", locale,
//                    cls.getClassLoader()), last);
//            }
//            return last;
//        };

    /**
     * Returns the name of the styling library or toolkit used by the portal.
     * This value is informative. It may, however, be used by
     * a {@link PageResourceProviderFactory} to influence the creation
     * of {@link PageResourceProvider}s.
     *
     * @return the value
     */
    public abstract String styling();

    /**
     * @return the prefix
     */
    public URI prefix() {
        return prefix;
    }

    /**
     * Returns the automatically generated {@link Portal} component.
     *
     * @return the portal
     */
    public Portal portal() {
        return portal;
    }

    /**
     * Sets the portal session network timeout. The portal session will be
     * removed if no messages have been received from the portal session
     * for the given number of milliseconds. The value defaults to 45 seconds.
     * 
     * @param timeout the timeout in milli seconds
     * @return the portal view for easy chaining
     */
    public PortalWeblet setPortalSessionNetworkTimeout(long timeout) {
        psNetworkTimeout = timeout;
        return this;
    }

    /**
     * Returns the portal session network timeout.
     *
     * @return the timeout
     */
    public long portalSessionNetworkTimeout() {
        return psNetworkTimeout;
    }

    /**
     * Sets the portal session refresh interval. The portal code in the
     * browser will send a keep alive packet if there has been no user
     * activity for more than the given number of milliseconds. The value 
     * defaults to 30 seconds.
     * 
     * @param interval the interval in milliseconds
     * @return the portal view for easy chaining
     */
    public PortalWeblet setPortalSessionRefreshInterval(long interval) {
        psRefreshInterval = interval;
        return this;
    }

    /**
     * Returns the portal session refresh interval.
     *
     * @return the interval
     */
    public long portalSessionRefreshInterval() {
        return psRefreshInterval;
    }

    /**
     * Sets the portal session inactivity timeout. If there has been no
     * user activity for more than the given number of milliseconds the
     * portal code stops sending keep alive packets and displays a
     * message to the user. The value defaults to -1 (no timeout).
     * 
     * @param timeout the timeout in milliseconds
     * @return the portal view for easy chaining
     */
    public PortalWeblet setPortalSessionInactivityTimeout(long timeout) {
        psInactivityTimeout = timeout;
        return this;
    }

    /**
     * Returns the portal session inactivity timeout.
     *
     * @return the timeout
     */
    public long portalSessionInactivityTimeout() {
        return psInactivityTimeout;
    }

    /**
     * Returns whether resources are minified.
     *
     * @return the useMinifiedResources
     */
    public boolean useMinifiedResources() {
        return useMinifiedResources;
    }

    /**
     * Determines if resources should be minified.
     *
     * @param useMinifiedResources the useMinifiedResources to set
     */
    public void setUseMinifiedResources(boolean useMinifiedResources) {
        this.useMinifiedResources = useMinifiedResources;
    }

    /**
     * Provides the render support.
     * 
     * @return the render support
     */
    protected RenderSupport renderSupport() {
        return renderSupport;
    }

    /**
     * Sets a function for obtaining a resource bundle for
     * a given locale.
     * 
     * @param supplier the function
     * @return the portal fo reasy chaining
     */
    public PortalWeblet setResourceBundleSupplier(
            Function<Locale, ResourceBundle> supplier) {
        resourceBundleSupplier = supplier;
        updateSupportedLocales();
        return this;
    }

    protected void updateSupportedLocales() {
        supportedLocales.clear();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals("")) {
                continue;
            }
            if (resourceBundleSupplier != null) {
                ResourceBundle bundle = resourceBundleSupplier.apply(locale);
                if (bundle.getLocale().equals(locale)) {
                    supportedLocales.add(locale);
                }
            }
            ResourceBundle bundle = ResourceBundle.getBundle(getClass()
                .getPackage().getName() + ".l10n", locale,
                getClass().getClassLoader());
            if (bundle.getLocale().equals(locale)) {
                supportedLocales.add(locale);
            }
        }
    }

    /**
     * Returns the resource bundle supplier.
     *
     * @return the result
     */
    protected Function<Locale, ResourceBundle> resourceBundleSupplier() {
        return resourceBundleSupplier;
    }

    /**
     * Returns the supported locales.
     *
     * @return the sets the
     */
    protected Set<Locale> supportedLocales() {
        return supportedLocales;
    }

    /**
     * Redirects `GET` requests without trailing slash.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @RequestHandler(dynamic = true)
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void onGetRedirect(GetRequest event, IOSubchannel channel)
            throws InterruptedException, IOException, ParseException {
        HttpResponse response = event.httpRequest().response().get();
        response.setStatus(HttpStatus.MOVED_PERMANENTLY)
            .setContentType("text", "plain", UTF_8)
            .setField(HttpField.LOCATION, prefix);
        channel.respond(new Response(response));
        try {
            channel.respond(Output.from(prefix.toString()
                .getBytes(UTF_8), true));
        } catch (UnsupportedEncodingException e) {
            // Supported by definition
        }
        event.setResult(true);
        event.stop();
    }

    /**
     * Handle the `GET` requests for the various resources.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @RequestHandler(dynamic = true)
    public void onGet(GetRequest event, IOSubchannel channel)
            throws InterruptedException, IOException, ParseException {
        URI requestUri = event.requestUri();
        int prefixSegs = requestPattern.matches(requestUri);
        // Request for portal? (Only valid with session)
        if (prefixSegs < 0 || !event.associated(Session.class).isPresent()) {
            return;
        }

        // Normalize and evaluate
        String requestPath = ResourcePattern.removeSegments(
            requestUri.getPath(), prefixSegs + 1);
        String[] requestParts = ResourcePattern.split(requestPath, 1);
        switch (requestParts[0]) {
        case "":
            // Because language is changed via websocket, locale cookie
            // may be out-dated
            event.associated(Selection.class)
                .ifPresent(selection -> selection.prefer(selection.get()[0]));
            // This is a portal session now (can be connected to)
            Session session = event.associated(Session.class).get();
            UUID portalSessionId = UUID.randomUUID();
            @SuppressWarnings("unchecked")
            Map<URI, UUID> knownIds = (Map<URI, UUID>) session.computeIfAbsent(
                PORTAL_SESSION_IDS,
                newKey -> new ConcurrentHashMap<URI, UUID>());
            knownIds.put(prefix, portalSessionId);
            // Finally render
            renderPortal(event, channel, portalSessionId);
            return;
        case "portal-resource":
            providePortalResource(event, ResourcePattern.removeSegments(
                requestUri.getPath(), prefixSegs + 2), channel);
            return;
        case "portal-base-resource":
            ResponseCreationSupport.sendStaticContent(event, channel,
                path -> PortalWeblet.class.getResource(requestParts[1]),
                null);
            return;
        case "page-resource":
            providePageResource(event, channel, requestParts[1]);
            return;
        case "portal-session":
            handleSessionRequest(event, channel, requestParts[1]);
            return;
        case "portlet-resource":
            providePortletResource(event, channel, URI.create(requestParts[1]));
            return;
        default:
            break;
        }
    }

    /**
     * Render the portal page.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    protected abstract void renderPortal(GetRequest event, IOSubchannel channel,
            UUID portalSessionId)
            throws IOException, InterruptedException;

    /**
     * Provide a portal resource. The implementation tries to load the
     * resource using {@link Class#getResource(String)} for each class
     * in the class hierarchy, starting with the finally derived class.
     *
     * @param event the event
     * @param requestPath the request path relativized to the 
     * common part for portal resources
     * @param channel the channel
     */
    protected void providePortalResource(GetRequest event,
            String requestPath, IOSubchannel channel) {
        for (Class<?> cls : portalResourceSearchSeq) {
            if (ResponseCreationSupport.sendStaticContent(event, channel,
                path -> cls.getResource(requestPath),
                null)) {
                break;
            }
        }
    }

    /**
     * Prepends the given class to the list of classes searched by
     * {@link #providePortalResource(GetRequest, String, IOSubchannel)}.
     * 
     * @param cls the class to prepend
     * @return the portal weblet for easy chaining
     */
    public PortalWeblet prependToResourceSearchPath(Class<?> cls) {
        portalResourceSearchSeq.add(0, cls);
        return this;
    }

    private void providePageResource(GetRequest event, IOSubchannel channel,
            String resource) throws InterruptedException {
        // Send events to providers on portal's channel
        PageResourceRequest pageResourceRequest = new PageResourceRequest(
            uriFromPath(resource),
            event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
                Converters.DATE_TIME).orElse(null),
            event.httpRequest(), channel, renderSupport());
        // Make session available (associate with event, this is not
        // a websocket request).
        event.associated(Session.class).ifPresent(
            session -> pageResourceRequest.setAssociated(Session.class,
                session));
        event.setResult(true);
        event.stop();
        fire(pageResourceRequest, portalChannel(channel));
    }

    private void providePortletResource(GetRequest event, IOSubchannel channel,
            URI resource) throws InterruptedException {
        String resPath = resource.getPath();
        int sep = resPath.indexOf('/');
        // Send events to portlets on portal's channel
        PortletResourceRequest portletRequest = new PortletResourceRequest(
            resPath.substring(0, sep),
            uriFromPath(resPath.substring(sep + 1)),
            event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
                Converters.DATE_TIME).orElse(null),
            event.httpRequest(), channel, renderSupport());
        // Make session available (associate with event, this is not
        // a websocket request).
        event.associated(Session.class).ifPresent(
            session -> portletRequest.setAssociated(Session.class, session));
        event.setResult(true);
        event.stop();
        fire(portletRequest, portalChannel(channel));
    }

    /**
     * The portal channel for getting resources. Resource providers
     * respond on the same event pipeline as they receive, because
     * handling is just a mapping to {@link ResourceRequestCompleted}.
     *
     * @param channel the channel
     * @return the IO subchannel
     */
    private IOSubchannel portalChannel(IOSubchannel channel) {
        @SuppressWarnings("unchecked")
        Optional<LinkedIOSubchannel> portalChannel
            = (Optional<LinkedIOSubchannel>) LinkedIOSubchannel
                .downstreamChannel(portal, channel);
        return portalChannel.orElseGet(
            () -> new PortalResourceChannel(
                portal, channel, activeEventPipeline()));
    }

    /**
     * Handles the {@link ResourceRequestCompleted} event.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    @Handler(channels = PortalChannel.class)
    public void onResourceRequestCompleted(
            ResourceRequestCompleted event, PortalResourceChannel channel)
            throws IOException, InterruptedException {
        event.stop();
        if (event.event().get() == null) {
            ResponseCreationSupport.sendResponse(event.event().httpRequest(),
                event.event().httpChannel(), HttpStatus.NOT_FOUND);
            return;
        }
        event.event().get().process();
    }

    private void handleSessionRequest(
            GetRequest event, IOSubchannel channel, String portalSessionId)
            throws InterruptedException, IOException, ParseException {
        // Must be WebSocket request.
        if (!event.httpRequest().findField(
            HttpField.UPGRADE, Converters.STRING_LIST)
            .map(fld -> fld.value().containsIgnoreCase("websocket"))
            .orElse(false)) {
            return;
        }

        // Can only connect to sessions that have been prepared
        // by loading the portal. (Prevents using a newly created
        // browser session from being (re-)connected to after a
        // long disconnect or restart and, of course, CSF).
        final Session browserSession = event.associated(Session.class).get();
        @SuppressWarnings("unchecked")
        Map<URI, UUID> knownIds
            = (Map<URI, UUID>) browserSession.computeIfAbsent(
                PORTAL_SESSION_IDS,
                newKey -> new ConcurrentHashMap<URI, UUID>());
        if (!UUID.fromString(portalSessionId) // NOPMD, note negation
            .equals(knownIds.get(prefix))) {
            channel.setAssociated(this, new String[2]);
        } else {
            channel.setAssociated(this, new String[] {
                portalSessionId,
                Optional.ofNullable(event.httpRequest().queryData()
                    .get("was")).map(vals -> vals.get(0)).orElse(null)
            });
        }
        channel.respond(new ProtocolSwitchAccepted(event, "websocket"));
        event.stop();
    }

    /**
     * Handles a change of Locale.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = PortalChannel.class)
    public void onSetLocale(SetLocale event, PortalSession channel)
            throws InterruptedException, IOException {
        Session session = channel.browserSession();
        if (session != null) {
            Selection selection = (Selection) session.get(Selection.class);
            if (selection != null) {
                supportedLocales.stream()
                    .filter(lang -> lang.equals(event.locale())).findFirst()
                    .ifPresent(lang -> selection.prefer(lang));
            }
        }
        channel.respond(new SimplePortalCommand("reload"));
    }

    /**
     * Called when the connection has been upgraded.
     *
     * @param event the event
     * @param wsChannel the ws channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onUpgraded(Upgraded event, IOSubchannel wsChannel)
            throws IOException {
        Optional<String[]> passedIn
            = wsChannel.associated(this, String[].class);
        if (!passedIn.isPresent()) {
            return;
        }

        // Check if reload required
        String[] portalSessionIds = passedIn.get();
        if (portalSessionIds[0] == null) {
            @SuppressWarnings("resource")
            CharBufferWriter out = new CharBufferWriter(wsChannel,
                wsChannel.responsePipeline()).suppressClose();
            new SimplePortalCommand("reload").toJson(out);
            out.close();
            event.stop();
            return;
        }

        // Get portal session
        final Session browserSession
            = wsChannel.associated(Session.class).get();
        // Reuse old portal session if still available
        PortalSession portalSession = Optional.ofNullable(portalSessionIds[1])
            .flatMap(opsId -> PortalSession.lookup(opsId))
            .map(session -> session.replaceId(portalSessionIds[0]))
            .orElse(PortalSession.lookupOrCreate(portalSessionIds[0],
                portal, psNetworkTimeout))
            .setUpstreamChannel(wsChannel)
            .setSession(browserSession);
        wsChannel.setAssociated(PortalSession.class, portalSession);
        // Channel now used as JSON input
        wsChannel.setAssociated(this, new WebSocketInputReader(
            event.processedBy().get(), portalSession));
        // From now on, only portalSession.respond may be used to send on the
        // upstream channel.
        portalSession.upstreamChannel().responsePipeline()
            .restrictEventSource(portalSession.responsePipeline());
    }

    /**
     * Handles network input (JSON data).
     *
     * @param event the event
     * @param wsChannel the ws channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onInput(Input<CharBuffer> event, IOSubchannel wsChannel)
            throws IOException {
        Optional<WebSocketInputReader> optWsInputReader
            = wsChannel.associated(this, WebSocketInputReader.class);
        if (optWsInputReader.isPresent()) {
            optWsInputReader.get().write(event.buffer().backingBuffer());
        }
    }

    /**
     * Handles the closed event from the web socket.
     *
     * @param event the event
     * @param wsChannel the WebSocket channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onClosed(
            Closed event, IOSubchannel wsChannel) throws IOException {
        Optional<WebSocketInputReader> optWsInputReader
            = wsChannel.associated(this, WebSocketInputReader.class);
        if (optWsInputReader.isPresent()) {
            wsChannel.setAssociated(this, null);
            optWsInputReader.get().close();
        }
        wsChannel.associated(PortalSession.class).ifPresent(session -> {
            // Restore channel to normal mode, see onPortalReady
            session.responsePipeline().restrictEventSource(null);
            session.disconnected();
        });
    }

    /**
     * Handles the {@link PortalReady} event.
     *
     * @param event the event
     * @param portalSession the portal session
     */
    @Handler(channels = PortalChannel.class)
    public void onPortalReady(PortalReady event, PortalSession portalSession) {
        String principal = Utils.userFromSession(portalSession.browserSession())
            .map(UserPrincipal::toString).orElse("");
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            "/" + principal + "/themeProvider", portalSession);
        fire(query, portalSession);
    }

    /**
     * Sends a command to the portal.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = PortalChannel.class, priority = -1000)
    public void onPortalCommand(
            PortalCommand event, PortalSession channel)
            throws InterruptedException, IOException {
        IOSubchannel upstream = channel.upstreamChannel();
        @SuppressWarnings("resource")
        CharBufferWriter out = new CharBufferWriter(upstream,
            upstream.responsePipeline()).suppressClose();
        event.toJson(out);
    }

    /**
     * Create a {@link URI} from a path. This is similar to calling
     * `new URI(null, null, path, null)` with the {@link URISyntaxException}
     * converted to a {@link IllegalArgumentException}.
     * 
     * @param path the path
     * @return the uri
     * @throws IllegalArgumentException if the string violates 
     * RFC 2396
     */
    public static URI uriFromPath(String path) throws IllegalArgumentException {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * The channel used to send {@link PageResourceRequest}s and
     * {@link PortletResourceRequest}s to the portlets (via the
     * portal).
     */
    public class PortalResourceChannel extends LinkedIOSubchannel {

        /**
         * Instantiates a new portal resource channel.
         *
         * @param hub the hub
         * @param upstreamChannel the upstream channel
         * @param responsePipeline the response pipeline
         */
        public PortalResourceChannel(Manager hub,
                IOSubchannel upstreamChannel, EventPipeline responsePipeline) {
            super(hub, hub.channel(), upstreamChannel, responsePipeline);
        }
    }

    /**
     * The implementation of {@link RenderSupport} used by this class.
     */
    private class RenderSupportImpl implements RenderSupport {

        @Override
        public URI portalBaseResource(URI uri) {
            return prefix.resolve(uriFromPath("portal-base-resource/"))
                .resolve(uri);
        }

        @Override
        public URI portalResource(URI uri) {
            return prefix.resolve(uriFromPath("portal-resource/")).resolve(uri);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jgrapes.portal.base.base.RenderSupport#portletResource(java.lang.
         * String,
         * java.net.URI)
         */
        @Override
        public URI portletResource(String portletType, URI uri) {
            return prefix.resolve(uriFromPath(
                "portlet-resource/" + portletType + "/")).resolve(uri);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jgrapes.portal.base.base.RenderSupport#pageResource(java.net.URI)
         */
        @Override
        public URI pageResource(URI uri) {
            return prefix.resolve(uriFromPath(
                "page-resource/")).resolve(uri);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jgrapes.portal.base.base.RenderSupport#useMinifiedResources()
         */
        @Override
        public boolean useMinifiedResources() {
            return useMinifiedResources;
        }

    }

}
