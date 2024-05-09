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
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Error;
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
import org.jgrapes.io.util.events.DataInput;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.ConsoleUser;

/**
 * Helper component for {@link LoginConlet} that handles the
 * communication with the OIDC provider.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class OidcClient extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final SecureRandom secureRandom = new SecureRandom();
    private Configuration config = new Configuration();
    private final Map<String, Context> contexts = new ConcurrentHashMap<>();

    /** For channel replacement. */
    private final class WebletChannel extends ClassChannel {
    }

    /**
     * Instantiates a new OIDC client.
     *
     * @param requesterChannel the requester channel
     * @param webletChannel the weblet channel
     * @param redirectTarget the redirect target
     */
    public OidcClient(Channel requesterChannel, Channel webletChannel,
            URI redirectTarget) {
        super(requesterChannel, ChannelReplacements.create()
            .add(WebletChannel.class, webletChannel));
        RequestHandler.Evaluator.add(this, "onAuthCallback",
            redirectTarget.toString());
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `clientId`
     * : The client id as defined in the OIDC provider.
     * `secret`
     * : The secret as defined in the OIDC provider.
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
            .setAssociated(this, new Context(event)));
    }

    /**
     * Invoked when the connection to the provider has been established.
     *
     * @param event the event
     * @param clientChannel the client channel
     */
    @Handler
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
    @Handler
    public void onResponse(Response response, IOSubchannel clientChannel)
            throws URISyntaxException {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        var rsp = (HttpResponse) response.response();
        clientChannel.setAssociated(HttpResponse.class, rsp);
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
    @Handler
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
    @Handler
    public void onDataInput(DataInput<Map<String, Object>> event,
            IOSubchannel clientChannel)
            throws MalformedURLException, URISyntaxException,
            JsonMappingException, JsonProcessingException {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        if (clientChannel.associated(HttpResponse.class)
            .map(r -> r.statusCode() != HttpURLConnection.HTTP_OK)
            .orElse(true)) {
            // Already handled in onResponse.
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

    private void attemptAuthorization(Context ctx)
            throws URISyntaxException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> params = new TreeMap<>();
        params.put("scope", "openid");
        params.put("response_type", "code");
        params.put("client_id", ctx.startEvent.provider().clientId());
        params.put("redirect_uri", config.redirectUri);
        params.put("prompt", "login");
        var stateBytes = new byte[16];
        secureRandom.nextBytes(stateBytes);
        var state = Base64.getEncoder().encodeToString(stateBytes);
        params.put("state", state);
        var request = ctx.startEvent.provider().authorizationEndpoint().toURI();
        request = HttpRequest.replaceQuery(request,
            HttpRequest.simpleWwwFormUrlencode(params));
        contexts.put(state, ctx);
        fire(new OpenLoginWindow(ctx.startEvent, request));
    }

    /**
     * On callback from the authorization request. (Path selector
     * defined in constructor.)
     *
     * @param event the event
     * @param channel the channel
     */
    @RequestHandler(channels = WebletChannel.class, dynamic = true)
    public void onAuthCallback(Request.In.Get event, IOSubchannel channel) {
        ResponseCreationSupport.sendStaticContent(event, channel,
            path -> getClass().getResource("CloseWindow.html"), null);
        var query = event.httpRequest().queryData();
        var ctx = Optional.ofNullable(query.get("state"))
            .orElse(Collections.emptyList()).stream().findFirst()
            .map(contexts::get).orElse(null);
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
            ctx));
    }

    @SuppressWarnings("unchecked")
    private void processTokenResponse(DataInput<Map<String, Object>> event,
            Context ctx, OidcProviderData provider) {
        ctx.idToken = JsonWebToken.parse((String) event.data().get("id_token"));
        var idData = ctx.idToken.payload();

        // Mandatory checks, see
        // https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
        if (provider.issuer() != null
            && !provider.issuer().toString().equals(idData.get("iss"))) {
            fire(new OidcError(ctx.startEvent, "ID token has invalid issuer."));
        }
        if (idData.get("aud") instanceof List auds && !auds.contains(
            provider.clientId())
            || idData.get("aud") instanceof String aud
                && !aud.equals(provider.clientId())) {
            fire(new OidcError(ctx.startEvent,
                "ID token has invalid audience."));
        }
        if (idData.get("exp") instanceof String exp
            && !Instant.now()
                .isBefore(Instant.ofEpochSecond(Long.valueOf(exp)))) {
            fire(new OidcError(ctx.startEvent, "ID token has expired."));
        }
        if (!idData.containsKey("preferred_username")) {
            fire(new OidcError(ctx.startEvent,
                "ID token does not contain preferred_username."));
        }

        // Success
        var user = new ConsoleUser((String) idData.get("preferred_username"),
            Optional.ofNullable((String) idData.get("name"))
                .orElse((String) idData.get("preferred_username")));
        var roles = new HashSet<ConsoleRole>();
        for (var role : (List<String>) idData.get("roles")) {
            roles.add(new ConsoleRole(role));
        }
        fire(new OidcUserAuthenticated(ctx.startEvent, user, roles));
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
    private class Context {
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
    }

}
