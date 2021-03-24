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

package org.jgrapes.webconsole.base;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Request.In.Get;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.webconsole.base.events.ConletResourceRequest;
import org.jgrapes.webconsole.base.events.ConsoleCommand;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.JsonInput;
import org.jgrapes.webconsole.base.events.PageResourceRequest;
import org.jgrapes.webconsole.base.events.ResourceRequestCompleted;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.events.SetLocaleCompleted;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;

/**
 * The server side base class for a web console single page 
 * application (SPA). Its two main tasks are to provide resources using 
 * {@link Request}/{@link Response} events (see 
 * {@link #onGet(org.jgrapes.http.events.Request.In.Get, IOSubchannel)}
 * for details about the different kinds of resources) and to convert
 * the JSON RPC messages received from the browser via the web socket
 * to {@link JsonInput} events and fire them on the
 * {@link ConsoleSession} channel.
 * 
 * The class has a counter part in the browser, the `jgconsole`
 * JavaScript module (see 
 * <a href="jsdoc/classes/console.html">functions</a>)
 * that can be loaded as `console-base-resource/jgconsole.js` 
 * (relative to the configured prefix). 
 * 
 * The class also provides handlers for some console related events
 * that affect the console representation in the browser.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public abstract class ConsoleWeblet extends Component {

    private static final String CONSOLE_SESSION_IDS
        = ConsoleWeblet.class.getName() + ".consoleSessionId";
    private static final String UTF_8 = "utf-8";

    private URI prefix;
    private final WebConsole console;
    private ResourcePattern requestPattern;

    private final RenderSupport renderSupport = new RenderSupportImpl();
    private boolean useMinifiedResources = true;
    private long psNetworkTimeout = 45000;
    private long psRefreshInterval = 30000;
    private long psInactivityTimeout = -1;

    private List<Class<?>> consoleResourceSearchSeq;
    private final List<Class<?>> resourceClasses = new ArrayList<>();
    private final ResourceBundle.Control resourceControl
        = new ConsoleResourceBundleControl(resourceClasses);
    private final Map<Locale, ResourceBundle> supportedLocales
        = new HashMap<>();

    /**
     * The class used in handler annotations to represent the 
     * console channel.
     */
    protected class ConsoleChannel extends ClassChannel {
    }

    /**
     * Instantiates a new console weblet. The weblet handles
     * {@link Get} events for URIs that start with the
     * specified prefix (see 
     * {@link #onGet(org.jgrapes.http.events.Request.In.Get, IOSubchannel)}).
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the console channel
     * @param consolePrefix the console prefix
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public ConsoleWeblet(Channel webletChannel, Channel consoleChannel,
            URI consolePrefix) {
        this(webletChannel, new WebConsole(consoleChannel));

        prefix = URI.create(consolePrefix.getPath().endsWith("/")
            ? consolePrefix.getPath()
            : consolePrefix.getPath() + "/");
        console.setView(this);

        String consolePath = prefix.getPath();
        if (consolePath.endsWith("/")) {
            consolePath = consolePath.substring(0, consolePath.length() - 1);
        }
        consolePath = consolePath + "|**";
        try {
            requestPattern = new ResourcePattern(consolePath);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        consoleResourceSearchSeq = consoleHierarchy();

        resourceClasses.addAll(consoleHierarchy());
        updateSupportedLocales();

        RequestHandler.Evaluator.add(this, "onGet", prefix + "**");
        RequestHandler.Evaluator.add(this, "onGetRedirect",
            prefix.getPath().substring(
                0, prefix.getPath().length() - 1));
    }

    private ConsoleWeblet(Channel webletChannel, WebConsole console) {
        super(webletChannel, ChannelReplacements.create()
            .add(ConsoleChannel.class, console.channel()));
        this.console = console;
        attach(console);
    }

    /**
     * Return the list of classes that form the current console
     * weblet implementation. This consists of all classes from
     * `getClass()` up to `ConsoleWeblet.class`. 
     *
     * @return the list
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected final List<Class<?>> consoleHierarchy() {
        List<Class<?>> result = new ArrayList<>();
        Class<?> derivative = getClass();
        while (true) {
            result.add(derivative);
            if (derivative.equals(ConsoleWeblet.class)) {
                break;
            }
            derivative = derivative.getSuperclass();
        }
        return result;
    }

    /**
     * Returns the name of the styling library or toolkit used by the console.
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
     * Returns the automatically generated {@link WebConsole} component.
     *
     * @return the console
     */
    public WebConsole console() {
        return console;
    }

    /**
     * Sets the console session network timeout. The console session will be
     * removed if no messages have been received from the console session
     * for the given number of milliseconds. The value defaults to 45 seconds.
     * 
     * @param timeout the timeout in milli seconds
     * @return the console view for easy chaining
     */
    public ConsoleWeblet setConsoleSessionNetworkTimeout(long timeout) {
        psNetworkTimeout = timeout;
        return this;
    }

    /**
     * Returns the console session network timeout.
     *
     * @return the timeout
     */
    public long consoleSessionNetworkTimeout() {
        return psNetworkTimeout;
    }

    /**
     * Sets the console session refresh interval. The console code in the
     * browser will send a keep alive packet if there has been no user
     * activity for more than the given number of milliseconds. The value 
     * defaults to 30 seconds.
     * 
     * @param interval the interval in milliseconds
     * @return the console view for easy chaining
     */
    public ConsoleWeblet setConsoleSessionRefreshInterval(long interval) {
        psRefreshInterval = interval;
        return this;
    }

    /**
     * Returns the console session refresh interval.
     *
     * @return the interval
     */
    public long consoleSessionRefreshInterval() {
        return psRefreshInterval;
    }

    /**
     * Sets the console session inactivity timeout. If there has been no
     * user activity for more than the given number of milliseconds the
     * console code stops sending keep alive packets and displays a
     * message to the user. The value defaults to -1 (no timeout).
     * 
     * @param timeout the timeout in milliseconds
     * @return the console view for easy chaining
     */
    public ConsoleWeblet setConsoleSessionInactivityTimeout(long timeout) {
        psInactivityTimeout = timeout;
        return this;
    }

    /**
     * Returns the console session inactivity timeout.
     *
     * @return the timeout
     */
    public long consoleSessionInactivityTimeout() {
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
     * Prepends a class to the list of classes used to lookup console
     * resources. See {@link ConsoleResourceBundleControl#newBundle}.
     * Affects the content of the resource bundle returned by
     * {@link #consoleResourceBundle(Locale)}. 
     * 
     * @param cls the class to prepend.
     * @return the console weblet for easy chaining
     */
    public ConsoleWeblet prependResourceBundleProvider(Class<?> cls) {
        resourceClasses.add(0, cls);
        updateSupportedLocales();
        return this;
    }

    /**
     * Update the supported locales.
     */
    protected final void updateSupportedLocales() {
        supportedLocales.clear();
        ResourceBundle.clearCache(ConsoleWeblet.class.getClassLoader());
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals("")) {
                continue;
            }
            ResourceBundle bundle = ResourceBundle.getBundle("l10n", locale,
                ConsoleWeblet.class.getClassLoader(), resourceControl);
            if (bundle.getLocale().equals(locale)) {
                supportedLocales.put(locale, bundle);
            }
        }
    }

    /**
     * Return the console resources for a given locale.
     *
     * @param locale the locale
     * @return the resource bundle
     */
    public ResourceBundle consoleResourceBundle(Locale locale) {
        return ResourceBundle.getBundle("l10n", locale,
            ConsoleWeblet.class.getClassLoader(), resourceControl);
    }

    /**
     * Returns the supported locales and their resource bundles.
     *
     * @return the set of locales supported by the console and their
     * resource bundles
     */
    protected Map<Locale, ResourceBundle> supportedLocales() {
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
    public void onGetRedirect(Request.In.Get event, IOSubchannel channel)
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
     * Handle the `GET` requests for the various resources. The requests
     * have to start with the prefix passed to the constructor. Further
     * processing depends on the next path segment:
     * 
     * * `.../console-base-resource`: Provide a resource associated
     *   with this class. The resources are:
     *   
     *   * `jgconsole.js`: The JavaScript module with helper classes
     *   * `console.css`: Some basic styles for conlets
     *
     * * `.../console-resource`: Invokes {@link #provideConsoleResource}
     *   with the remainder of the path.
     *   
     * * `.../page-resource`: Invokes {@link #providePageResource}
     *   with the remainder of the path.
     *   
     * * `.../conlet-resource`: Invokes {@link #provideConletResource}
     *   with the remainder of the path.
     *   
     * * `.../console-session`: Handled by this class. Used
     *   e.g. for initiating the web socket connection.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @RequestHandler(dynamic = true)
    public void onGet(Request.In.Get event, IOSubchannel channel)
            throws InterruptedException, IOException, ParseException {
        URI requestUri = event.requestUri();
        int prefixSegs = requestPattern.matches(requestUri);
        // Request for console? (Only valid with session)
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
            // This is a console session now (can be connected to)
            Session session = event.associated(Session.class).get();
            UUID consoleSessionId = UUID.randomUUID();
            @SuppressWarnings("unchecked")
            Map<URI, UUID> knownIds = (Map<URI, UUID>) session.computeIfAbsent(
                CONSOLE_SESSION_IDS,
                newKey -> new ConcurrentHashMap<URI, UUID>());
            knownIds.put(prefix, consoleSessionId);
            // Finally render
            renderConsole(event, channel, consoleSessionId);
            return;
        case "console-resource":
            provideConsoleResource(event, ResourcePattern.removeSegments(
                requestUri.getPath(), prefixSegs + 2), channel);
            return;
        case "console-base-resource":
            ResponseCreationSupport.sendStaticContent(event, channel,
                path -> ConsoleWeblet.class.getResource(requestParts[1]),
                null);
            return;
        case "page-resource":
            providePageResource(event, channel, requestParts[1]);
            return;
        case "console-session":
            handleSessionRequest(event, channel, requestParts[1]);
            return;
        case "conlet-resource":
            provideConletResource(event, channel, URI.create(requestParts[1]));
            return;
        default:
            break;
        }
    }

    /**
     * Render the console page.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    protected abstract void renderConsole(Request.In.Get event,
            IOSubchannel channel, UUID consoleSessionId)
            throws IOException, InterruptedException;

    /**
     * Provide a console resource. The implementation tries to load the
     * resource using {@link Class#getResource(String)} for each class
     * in the class hierarchy, starting with the finally derived class.
     *
     * @param event the event
     * @param requestPath the request path relativized to the 
     * common part for console resources
     * @param channel the channel
     */
    protected void provideConsoleResource(Request.In.Get event,
            String requestPath, IOSubchannel channel) {
        for (Class<?> cls : consoleResourceSearchSeq) {
            if (ResponseCreationSupport.sendStaticContent(event, channel,
                path -> cls.getResource(requestPath),
                null)) {
                break;
            }
        }
    }

    /**
     * Prepends the given class to the list of classes searched by
     * {@link #provideConsoleResource(Request.In.Get, String, IOSubchannel)}.
     * 
     * @param cls the class to prepend
     * @return the console weblet for easy chaining
     */
    public ConsoleWeblet prependConsoleResourceProvider(Class<?> cls) {
        consoleResourceSearchSeq.add(0, cls);
        return this;
    }

    private void providePageResource(Request.In.Get event, IOSubchannel channel,
            String resource) throws InterruptedException {
        // Send events to providers on console's channel
        PageResourceRequest pageResourceRequest = new PageResourceRequest(
            WebConsoleUtils.uriFromPath(resource),
            event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
                Converters.DATE_TIME).orElse(null),
            event.httpRequest(), channel, event.associated(Session.class).get(),
            renderSupport());
        event.setResult(true);
        event.stop();
        fire(pageResourceRequest, consoleChannel(channel));
    }

    private void provideConletResource(Request.In.Get event,
            IOSubchannel channel,
            URI resource) throws InterruptedException {
        try {
            String resPath = resource.getPath();
            int sep = resPath.indexOf('/');
            // Send events to web console components on console's channel
            ConletResourceRequest conletRequest
                = new ConletResourceRequest(
                    resPath.substring(0, sep),
                    new URI(null, null, resPath.substring(sep + 1),
                        event.requestUri().getQuery(),
                        event.requestUri().getFragment()),
                    event.httpRequest().findValue(HttpField.IF_MODIFIED_SINCE,
                        Converters.DATE_TIME).orElse(null),
                    event.httpRequest(), channel,
                    event.associated(Session.class).get(), renderSupport());
            // Make session available (associate with event, this is not
            // a websocket request).
            event.associated(Session.class).ifPresent(
                session -> conletRequest.setAssociated(Session.class,
                    session));
            event.setResult(true);
            event.stop();
            fire(conletRequest, consoleChannel(channel));
        } catch (URISyntaxException e) {
            // Won't happen, new URI derived from existing
        }
    }

    /**
     * The console channel for getting resources. Resource providers
     * respond on the same event pipeline as they receive, because
     * handling is just a mapping to {@link ResourceRequestCompleted}.
     *
     * @param channel the channel
     * @return the IO subchannel
     */
    private IOSubchannel consoleChannel(IOSubchannel channel) {
        @SuppressWarnings("unchecked")
        Optional<LinkedIOSubchannel> consoleChannel
            = (Optional<LinkedIOSubchannel>) LinkedIOSubchannel
                .downstreamChannel(console, channel);
        return consoleChannel.orElseGet(
            () -> new ConsoleResourceChannel(
                console, channel, activeEventPipeline()));
    }

    /**
     * Handles the {@link ResourceRequestCompleted} event.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    @Handler(channels = ConsoleChannel.class)
    public void onResourceRequestCompleted(
            ResourceRequestCompleted event, ConsoleResourceChannel channel)
            throws IOException, InterruptedException {
        event.stop();
        if (event.event().get() == null) {
            ResponseCreationSupport.sendResponse(event.event().httpRequest(),
                event.event().httpChannel(), HttpStatus.NOT_FOUND);
            return;
        }
        event.event().get().process();
    }

    private void handleSessionRequest(Request.In.Get event,
            IOSubchannel channel, String consoleSessionId)
            throws InterruptedException, IOException, ParseException {
        // Must be WebSocket request.
        if (!event.httpRequest().findField(
            HttpField.UPGRADE, Converters.STRING_LIST)
            .map(fld -> fld.value().containsIgnoreCase("websocket"))
            .orElse(false)) {
            return;
        }

        // Can only connect to sessions that have been prepared
        // by loading the console. (Prevents using a newly created
        // browser session from being (re-)connected to after a
        // long disconnect or restart and, of course, CSF).
        final Session browserSession = event.associated(Session.class).get();
        @SuppressWarnings("unchecked")
        Map<URI, UUID> knownIds
            = (Map<URI, UUID>) browserSession.computeIfAbsent(
                CONSOLE_SESSION_IDS,
                newKey -> new ConcurrentHashMap<URI, UUID>());
        if (!UUID.fromString(consoleSessionId) // NOPMD, note negation
            .equals(knownIds.get(prefix))) {
            channel.setAssociated(this, new String[2]);
        } else {
            channel.setAssociated(this, new String[] {
                consoleSessionId,
                Optional.ofNullable(event.httpRequest().queryData()
                    .get("was")).map(vals -> vals.get(0)).orElse(null)
            });
        }
        channel.respond(new ProtocolSwitchAccepted(event, "websocket"));
        event.stop();
    }

    /**
     * Handles a change of Locale for the console.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = ConsoleChannel.class, priority = 10000)
    public void onSetLocale(SetLocale event, ConsoleSession channel)
            throws InterruptedException, IOException {
        channel.setLocale(event.locale());
        Session session = channel.browserSession();
        if (session != null) {
            Selection selection = (Selection) session.get(Selection.class);
            if (selection != null) {
                supportedLocales.keySet().stream()
                    .filter(lang -> lang.equals(event.locale())).findFirst()
                    .ifPresent(lang -> selection.prefer(lang));
            }
        }
        if (event.reload()) {
            channel.respond(new SimpleConsoleCommand("reload"));
        }
    }

    /**
     * Sends a reload if the change of locale could not be handled by
     * all portelts.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(channels = ConsoleChannel.class)
    public void onSetLocaleCompleted(SetLocaleCompleted event,
            ConsoleSession channel) {
        if (event.event().reload()) {
            channel.respond(new SimpleConsoleCommand("reload"));
        }
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
        String[] consoleSessionIds = passedIn.get();
        if (consoleSessionIds[0] == null) {
            @SuppressWarnings("resource")
            CharBufferWriter out = new CharBufferWriter(wsChannel,
                wsChannel.responsePipeline()).suppressClose();
            new SimpleConsoleCommand("reload").toJson(out);
            out.close();
            event.stop();
            return;
        }

        // Get console session
        final Session browserSession
            = wsChannel.associated(Session.class).get();
        // Reuse old console session if still available
        ConsoleSession consoleSession
            = Optional.ofNullable(consoleSessionIds[1])
                .flatMap(opsId -> ConsoleSession.lookup(opsId))
                .map(session -> session.replaceId(consoleSessionIds[0]))
                .orElse(ConsoleSession.lookupOrCreate(consoleSessionIds[0],
                    console, supportedLocales.keySet(), psNetworkTimeout))
                .setUpstreamChannel(wsChannel)
                .setSession(browserSession);
        wsChannel.setAssociated(ConsoleSession.class, consoleSession);
        // Channel now used as JSON input
        wsChannel.setAssociated(this, new WebSocketInputReader(
            event.processedBy().get(), consoleSession));
        // From now on, only consoleSession.respond may be used to send on the
        // upstream channel.
        consoleSession.upstreamChannel().responsePipeline()
            .restrictEventSource(consoleSession.responsePipeline());
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
        wsChannel.associated(ConsoleSession.class).ifPresent(session -> {
            // Restore channel to normal mode, see onConsoleReady
            session.responsePipeline().restrictEventSource(null);
            session.disconnected();
        });
    }

    /**
     * Handles the {@link ConsoleReady} event.
     *
     * @param event the event
     * @param consoleSession the console session
     */
    @Handler(channels = ConsoleChannel.class)
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession) {
        String principal
            = WebConsoleUtils.userFromSession(consoleSession.browserSession())
                .map(UserPrincipal::toString).orElse("");
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            "/" + principal + "/themeProvider", consoleSession);
        fire(query, consoleSession);
    }

    /**
     * Sends a command to the console.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = ConsoleChannel.class, priority = -1000)
    public void onConsoleCommand(
            ConsoleCommand event, ConsoleSession channel)
            throws InterruptedException, IOException {
        IOSubchannel upstream = channel.upstreamChannel();
        @SuppressWarnings("resource")
        CharBufferWriter out = new CharBufferWriter(upstream,
            upstream.responsePipeline()).suppressClose();
        event.toJson(out);
    }

    /**
     * The channel used to send {@link PageResourceRequest}s and
     * {@link ConletResourceRequest}s to the web console components (via the
     * console).
     */
    public class ConsoleResourceChannel extends LinkedIOSubchannel {

        /**
         * Instantiates a new console resource channel.
         *
         * @param hub the hub
         * @param upstreamChannel the upstream channel
         * @param responsePipeline the response pipeline
         */
        public ConsoleResourceChannel(Manager hub,
                IOSubchannel upstreamChannel, EventPipeline responsePipeline) {
            super(hub, hub.channel(), upstreamChannel, responsePipeline);
        }
    }

    /**
     * The implementation of {@link RenderSupport} used by this class.
     */
    private class RenderSupportImpl implements RenderSupport {

        @Override
        public URI consoleBaseResource(URI uri) {
            return prefix
                .resolve(WebConsoleUtils.uriFromPath("console-base-resource/"))
                .resolve(uri);
        }

        @Override
        public URI consoleResource(URI uri) {
            return prefix
                .resolve(WebConsoleUtils.uriFromPath("console-resource/"))
                .resolve(uri);
        }

        @Override
        public URI conletResource(String conletType, URI uri) {
            return prefix.resolve(WebConsoleUtils.uriFromPath(
                "conlet-resource/" + conletType + "/")).resolve(uri);
        }

        @Override
        public URI pageResource(URI uri) {
            return prefix.resolve(WebConsoleUtils.uriFromPath(
                "page-resource/")).resolve(uri);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jgrapes.webconsole.base.base.RenderSupport#useMinifiedResources()
         */
        @Override
        public boolean useMinifiedResources() {
            return useMinifiedResources;
        }

    }

}
