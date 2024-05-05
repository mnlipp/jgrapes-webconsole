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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Error;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.HttpConnected;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.JsonReader;
import org.jgrapes.io.util.events.DataInput;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * Helper component for {@link LoginConlet} that handles the
 * communication with the OIDC provider.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class OidcClient extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final SecureRandom secureRandom = new SecureRandom();
    private Configuration config = new Configuration();

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
        RequestHandler.Evaluator.add(this, "onGet",
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
     * @throws MalformedURLException the malformed URL exception
     */
    @Handler
    public void onStartProviderLogin(StartOidcLogin event)
            throws MalformedURLException {
        var providerData = event.provider();
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
        // Transfer context to the new subchannel.
        event.request().associated(this, Context.class).ifPresent(c -> {
            clientChannel.setAssociated(this, c);
        });
    }

    /**
     * Invoked when a response is received from the provider.
     *
     * @param response the response
     * @param clientChannel the client channel
     */
    @Handler
    public void onResponse(Response response, IOSubchannel clientChannel) {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        var hdr = (HttpResponse) response.response();
        if (hdr.statusCode() != HttpURLConnection.HTTP_OK) {
            fire(new Error(response, "Attempt to access configuration"
                + " endpoint returned " + hdr.statusCode()));
            return;
        }
        var context = optCtx.get();
        context.jsonReader = new JsonReader(Map.class, activeEventPipeline(),
            clientChannel);
        context.jsonReader
            .charset(response.charset().orElse(StandardCharsets.UTF_8));
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
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        optCtx.get().jsonReader.feed(event.buffer());
    }

    /**
     * On data input.
     *
     * @param event the event
     * @param clientChannel the client channel
     * @throws MalformedURLException the malformed URL exception
     * @throws URISyntaxException the URI syntax exception
     */
    @Handler
    public void onDataInput(DataInput<Map<String, Object>> event,
            IOSubchannel clientChannel)
            throws MalformedURLException, URISyntaxException {
        var optCtx = clientChannel.associated(this, Context.class);
        if (optCtx.isEmpty()) {
            return;
        }
        var provider = optCtx.get().startEvent.provider();
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

        openLoginWindow(optCtx.get().startEvent,
            provider.authorizationEndpoint(), clientChannel);
    }

    private void openLoginWindow(StartOidcLogin forLogin, URL aep,
            IOSubchannel clientChannel) throws URISyntaxException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> params = new TreeMap<>();
        params.put("scope", "openid");
        params.put("response_type", "code");
        params.put("client_id", config.clientId);
        params.put("redirect_uri", config.redirectUri);
        var stateBytes = new byte[16];
        secureRandom.nextBytes(stateBytes);
        var state = Base64.getEncoder().encodeToString(stateBytes);
        params.put("state", state);
        var query = params.entrySet().stream().map(e -> e.getKey() + "="
            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .reduce((p1, p2) -> p1 + "&" + p2).map(s -> "?" + s).orElse("");
        var request = new URI(new URI(aep.getProtocol(), aep.getAuthority(),
            aep.getPath(), null, null) + query);
        clientChannel.respond(new OpenLoginWindow(forLogin, request));
    }

    @RequestHandler(channels = WebletChannel.class, dynamic = true)
    public void onGet(Request.In.Get event, IOSubchannel channel) {
        int i = 0;
    }

    /**
     * The configuration information.
     */
    public static class Configuration {
        public String clientId;
        public String secret;
        public String redirectUri;
    }

    /**
     * The context information.
     */
    private class Context {
        public final StartOidcLogin startEvent;
        public JsonReader jsonReader;

        public Context(StartOidcLogin startEvent) {
            super();
            this.startEvent = startEvent;
        }
    }

}
