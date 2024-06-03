/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

package org.jgrapes.webconlet.oidclogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Error;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.HttpConnected;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.InputConsumer;
import org.jgrapes.io.util.JsonReader;
import org.jgrapes.io.util.OutputSupplier;
import org.jgrapes.io.util.events.DataInput;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.webconlet.oidclogin.OidcError.Kind;
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.ConsoleUser;
import org.jgrapes.webconsole.base.events.UserAuthenticated;

/**
 * Helper component for {@link LoginConlet} that handles the
 * communication with the OIDC provider. "OidcClient" is a bit
 * of a misnomer because this class not only initiates requests
 * to the OIDC provider but also serves the redirect URI that the
 * provider uses as callback. However, the callback can be seen 
 * as the asynchronous response to the authentication request
 * that the {@link OidcClient} sends initially, therefore the
 * component primarily acts as a client nevertheless.
 * 
 * The component requires a HTTP server component (usually an
 * instance of {@link HttpServer}) to exist that handles the 
 * {@link Request.Out} events that this component fires. The HTTP
 * server must also convert the calls to the redirect URI
 * from the provider to a {@link Request.In.Get} event. Details 
 * about configuring the various channels used can be found in the
 * {@link #OidcClient description of the constructor}.
 * 
 * The component has a single configuration property that sets
 * the value of the redirect URI sent to the OIDC provider.
 * ```yaml
 * "...":
 *   "/OidcClient":
 *     redirectUri: "https://localhost:5443/vjconsole/oauth/callback"
 * ```
 * 
 * While it is tempting to simply use as redirect URI the host/port
 * from the HTTP server component together with the request path
 * passed to the constructor, there are two reasons why the redirect
 * URI has to be configured explicitly. First, the framework does
 * not support querying the host/port properties from the server
 * component. Second, and more import, the HTTP server component will
 * often be placed behind a firewall or reverse proxy and therefore
 * the URL that it serves will usually differ from the redirect
 * URI sent to the OIDC provider.
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.ExcessiveImports",
    "PMD.CouplingBetweenObjects", "PMD.GodClass" })
public class OidcClient extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final SecureRandom secureRandom = new SecureRandom();
    private Configuration config = new Configuration();
    private final Map<String, Context> contexts = new ConcurrentHashMap<>();
    private final Channel httpClientChannel;

    /** For channel replacement. */
    private final class HttpClientChannel extends ClassChannel {
    }

    /** For channel replacement. */
    private final class HttpServerChannel extends ClassChannel {
    }

    /**
     * Instantiates a new OIDC client. The OIDC uses three channels.
     * 
     *  * It is a helper component for the {@link LoginConlet} and
     *    therefore uses its "primary" (component) channel to 
     *    exchange events with the conlet.
     *    
     *  * It uses the `httpClientChannel` to accesses the provider
     *    as client, i.e. when it fires {@link Request.Out}
     *    events to obtain information from the provider.
     *    
     *  * It defines a request handler that listens on the
     *    `httpServerChannel` for handling the authorization callback
     *    from the provider. 
     *
     * @param componentChannel the component's channel
     * @param httpClientChannel the channel used for connecting the provider
     * @param httpServerChannel the channel used by some {@link HttpServer}
     * to send the {@link Request.In} events from the provider callback
     * @param redirectTarget defines the path handled by 
     * {@link #onAuthCallback}
     * @param priority the priority of the {@link #onAuthCallback} handler.
     * Must be higher than the default priority of request handlers if the
     * callback URL uses a sub-path of the web console's URL.
     */
    public OidcClient(Channel componentChannel, Channel httpClientChannel,
            Channel httpServerChannel, URI redirectTarget, int priority) {
        super(componentChannel, ChannelReplacements.create()
            .add(HttpClientChannel.class, httpClientChannel)
            .add(HttpServerChannel.class, httpServerChannel));
        this.httpClientChannel = httpClientChannel;
        RequestHandler.Evaluator.add(this, "onAuthCallback",
            redirectTarget.toString(), priority);
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `redirectUri`
     * : The redirect URI as defined in the OIDC provider.
     * 
     * @param event the event
     */
    @Handler
    public void onConfigUpdate(ConfigurationUpdate event) {
        event.structured(componentPath()).ifPresent(m -> {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.convertValue(m, Configuration.class);
        });
    }

    /**
     * On start provider login.
     *
     * @param event the event
     * @throws URISyntaxException 
     */
    @Handler
    public void onStartProviderLogin(StartOidcLogin event)
            throws URISyntaxException {
        var providerData = event.provider();
        if (providerData.authorizationEndpoint() != null
            && providerData.tokenEndpoint() != null) {
            attemptAuthorization(new Context(event));
            return;
        }
        // Get configuration information first
        fire(new Request.Out.Get(providerData.configurationEndpoint())
            .setAssociated(this, new Context(event)), httpClientChannel);
    }

    /**
     * Invoked when the connection to the provider has been established.
     *
     * @param event the event
     * @param clientChannel the client channel
     */
    @Handler(channels = HttpClientChannel.class)
    public void onConnected(HttpConnected event, IOSubchannel clientChannel) {
        // Transfer context from the request to the new subchannel.
        event.request().associated(this, Context.class).ifPresent(c -> {
            clientChannel.setAssociated(this, c);
            // Also keep the request in order to dispatch responses
            clientChannel.setAssociated(HttpRequest.class,
                event.request().httpRequest());
            // Send body if provided together with the request
            event.request().associated(OutputSupplier.class)
                .ifPresent(bp -> bp.emit(clientChannel));
        });
    }

    /**
     * Invoked when a response is received from the provider.
     *
     * @param response the response
     * @param clientChannel the client channel
     * @throws URISyntaxException 
     */
    @Handler(channels = HttpClientChannel.class)
    public void onResponse(Response response, IOSubchannel clientChannel)
            throws URISyntaxException {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        var rsp = (HttpResponse) response.response();
        clientChannel.setAssociated(Response.class, response);
        var reqUri
            = clientChannel.associated(HttpRequest.class).get().requestUri();
        if (rsp.statusCode() != HttpURLConnection.HTTP_OK) {
            fire(new Error(response, "Attempt to access " + reqUri
                + " returned " + rsp.statusCode()));
        }
        // All expected responses have a JSON payload (body), so
        // wait for it and delay dispatching until the data is available.
        if (rsp.hasPayload()) {
            clientChannel.setAssociated(InputConsumer.class,
                new JsonReader(Map.class, activeEventPipeline(), clientChannel)
                    .charset(
                        response.charset().orElse(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Collect and process input from the provider.
     *
     * @param event the event
     * @param clientChannel the client channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = HttpClientChannel.class)
    public void onInput(Input<ByteBuffer> event, IOSubchannel clientChannel)
            throws IOException {
        if (clientChannel.associated(this, Context.class).isEmpty()) {
            return;
        }
        clientChannel.associated(InputConsumer.class).ifPresent(
            ic -> ic.feed(event));
    }

    /**
     * On data input.
     *
     * @param event the event
     * @param clientChannel the client channel
     * @throws MalformedURLException the malformed URL exception
     * @throws URISyntaxException the URI syntax exception
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Handler(channels = HttpClientChannel.class)
    public void onDataInput(DataInput<Map<String, Object>> event,
            IOSubchannel clientChannel)
            throws MalformedURLException, URISyntaxException,
            JsonMappingException, JsonProcessingException {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        if (clientChannel.associated(Response.class)
            .map(r -> (HttpResponse) r.response())
            .map(r -> r.statusCode() != HttpURLConnection.HTTP_OK)
            .orElse(true)) {
            // Already handled in onResponse, log details
            Response evt = clientChannel.associated(Response.class).get();
            logger.finer(() -> "Payload of failed event "
                + Components.objectName(evt) + ": " + event.data().toString());
            return;
        }
        var provider = optCtx.get().startEvent.provider();
        // Dispatch based on information from the request URI
        var reqUri
            = clientChannel.associated(HttpRequest.class).get().requestUri();
        if (reqUri.equals(provider.configurationEndpoint().toURI())) {
            processConfigurationData(event, optCtx.get(), provider);
        } else if (reqUri.equals(provider.tokenEndpoint().toURI())) {
            processTokenResponse(event, optCtx.get(), provider);
        }
    }

    private void processConfigurationData(DataInput<Map<String, Object>> event,
            Context ctx, OidcProviderData provider)
            throws MalformedURLException, URISyntaxException {
        String aep = (String) event.data().get("authorization_endpoint");
        if (aep != null) {
            provider.setAuthorizationEndpoint(new URL(aep));
        }
        String tep = (String) event.data().get("token_endpoint");
        if (tep != null) {
            provider.setTokenEndpoint(new URL(tep));
        }
        String uiep = (String) event.data().get("userinfo_endpoint");
        if (uiep != null) {
            provider.setUserinfoEndpoint(new URL(uiep));
        }
        String issuer = (String) event.data().get("issuer");
        if (issuer != null) {
            provider.setIssuer(new URL(issuer));
        }

        // We only get the configuration information as part of a login
        // process, so continue with the login now.
        attemptAuthorization(ctx);
    }

    private void attemptAuthorization(Context ctx) throws URISyntaxException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> params = new TreeMap<>();
        params.put("scope", "openid");
        params.put("response_type", "code");
        params.put("client_id", ctx.startEvent.provider().clientId());
        params.put("redirect_uri", config.redirectUri);
        params.put("prompt", "login");
        if (ctx.startEvent.locales().length > 0) {
            params.put("ui_locales", Arrays.stream(ctx.startEvent.locales())
                .map(Locale::toLanguageTag).collect(Collectors.joining(" ")));
        }
        var stateBytes = new byte[16];
        secureRandom.nextBytes(stateBytes);
        var state = Base64.getEncoder().encodeToString(stateBytes);
        params.put("state", state);
        var request = HttpRequest.replaceQuery(
            ctx.startEvent.provider().authorizationEndpoint().toURI(),
            HttpRequest.simpleWwwFormUrlencode(params));
        logger.finer(() -> "Getting " + request);
        contexts.put(state, ctx);
        fire(new OpenLoginWindow(ctx.startEvent, request));

        // Simple purging of left overs
        var ageLimit = Instant.now().minusSeconds(60);
        for (var itr = contexts.entrySet().iterator(); itr.hasNext();) {
            var entry = itr.next();
            if (entry.getValue().createdAt().isBefore(ageLimit)) {
                itr.remove();
            }
        }
    }

    /**
     * On callback from the authorization request. (Path selector
     * defined in constructor.)
     *
     * @param event the event
     * @param channel the channel
     */
    @RequestHandler(channels = HttpServerChannel.class, dynamic = true)
    public void onAuthCallback(Request.In.Get event, IOSubchannel channel) {
        ResponseCreationSupport.sendStaticContent(event, channel,
            path -> getClass().getResource("CloseWindow.html"), null);
        var query = event.httpRequest().queryData();
        var state = Optional.ofNullable(query.get("state"))
            .orElse(Collections.emptyList()).stream().findFirst().orElse(null);
        var ctx = contexts.remove(state);
        if (ctx == null) {
            return;
        }
        ctx.code = query.get("code").get(0);

        // Prepare token request
        OidcProviderData provider = ctx.startEvent.provider();
        Request.Out.Post post = new Request.Out.Post(provider.tokenEndpoint());
        post.httpRequest().setField(HttpField.CONTENT_TYPE,
            new MediaType("application", "x-www-form-urlencoded"));
        post.httpRequest().setField(HttpField.AUTHORIZATION,
            new ParameterizedValue<>("Basic "
                + Base64.getEncoder().encodeToString(
                    (provider.clientId() + ":" + provider.secret())
                        .getBytes())));
        post.httpRequest().setHasPayload(true);

        // Prepare payload data
        var params = new TreeMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", ctx.code);
        params.put("redirect_uri", config.redirectUri);
        @SuppressWarnings("resource")
        OutputSupplier body = (IOSubchannel c) -> {
            new CharBufferWriter(c, activeEventPipeline())
                .append(HttpRequest.simpleWwwFormUrlencode(params)).close();
        };
        fire(post.setAssociated(OutputSupplier.class, body).setAssociated(this,
            ctx), httpClientChannel);
        event.setResult(true);
        event.stop();
    }

    @SuppressWarnings({ "unchecked", "PMD.NPathComplexity",
        "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity" })
    private void processTokenResponse(DataInput<Map<String, Object>> event,
            Context ctx, OidcProviderData provider) {
        ctx.idToken = JsonWebToken.parse((String) event.data().get("id_token"));
        var idData = ctx.idToken.payload();

        // Mandatory checks, see
        // https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
        if (provider.issuer() != null
            && !provider.issuer().toString().equals(idData.get("iss"))) {
            fire(new OidcError(ctx.startEvent, Kind.INVALID_ISSUER,
                "ID token has invalid issuer."));
            event.stop();
            return;
        }
        if (idData.get("aud") instanceof List auds && !auds.contains(
            provider.clientId()) || idData.get("aud") instanceof String aud
                && !aud.equals(provider.clientId())) {
            fire(new OidcError(ctx.startEvent, Kind.INVALID_AUDIENCE,
                "ID token has invalid audience."));
            event.stop();
            return;
        }
        if (idData.get("exp") instanceof Integer exp
            && !Instant.now().isBefore(Instant.ofEpochSecond(exp))) {
            fire(new OidcError(ctx.startEvent, Kind.ID_TOKEN_EXPIRED,
                "ID token has expired."));
            event.stop();
            return;
        }
        if (!idData.containsKey("preferred_username")) {
            fire(new OidcError(ctx.startEvent, Kind.PREFERRED_USERNAME_MISSING,
                "ID token does not contain preferred_username."));
            event.stop();
            return;
        }

        // Check if allowed
        var roles = Optional.ofNullable((List<String>) idData.get("roles"))
            .orElse(Collections.emptyList());
        if (!(roles.isEmpty() && provider.authorizedRoles().contains(""))
            && !roles.stream().filter(r -> provider.authorizedRoles()
                .contains(r)).findAny().isPresent()) {
            // Not allowed
            fire(new OidcError(ctx.startEvent, Kind.ACCESS_DENIED,
                "Access denied (no allowed role)."));
            event.stop();
            return;
        }

        // Success

        Subject subject = new Subject();
        var user = new ConsoleUser(
            mapName((String) idData.get("preferred_username"),
                provider.userMappings(), provider.patternCache()),
            Optional.ofNullable((String) idData.get("name"))
                .orElse((String) idData.get("preferred_username")));
        if (idData.containsKey("email")) {
            try {
                user.setEmail(
                    new InternetAddress((String) idData.get("email")));
            } catch (AddressException e) {
                logger.log(Level.WARNING, e,
                    () -> "Failed to parse email address \""
                        + idData.get("email") + "\": " + e.getMessage());
            }
        }
        subject.getPrincipals().add(user);
        for (var role : roles) {
            subject.getPrincipals().add(new ConsoleRole(mapName(role,
                provider.roleMappings(), provider.patternCache()), role));
        }
        fire(new UserAuthenticated(ctx.startEvent, subject).by(
            "OIDC Provider " + provider.name()));
    }

    private String mapName(String name, List<Map<String, String>> mappings,
            Map<String, Pattern> patternCache) {
        for (var mapping : mappings) {
            @SuppressWarnings("PMD.LambdaCanBeMethodReference")
            var pattern = patternCache.computeIfAbsent(mapping.get("from"),
                k -> Pattern.compile(k));
            var matcher = pattern.matcher(name);
            if (matcher.matches()) {
                return matcher.replaceFirst(mapping.get("to"));
            }
        }
        return name;
    }

    /**
     * The configuration information.
     */
    public static class Configuration {
        public String redirectUri;
    }

    /**
     * The context information.
     */
    @SuppressWarnings("PMD.DataClass")
    private class Context {
        private final Instant createdAt = Instant.now();
        public final StartOidcLogin startEvent;
        public String code;
        public JsonWebToken idToken;

        /**
         * Instantiates a new context.
         *
         * @param startEvent the start event
         */
        public Context(StartOidcLogin startEvent) {
            super();
            this.startEvent = startEvent;
        }

        /**
         * Created at.
         *
         * @return the instant
         */
        public Instant createdAt() {
            return createdAt;
        }
    }

}
