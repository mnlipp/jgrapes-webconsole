/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import javax.security.auth.Subject;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.io.events.Close;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConletBaseModel;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.ConsoleUser;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.CloseModalDialog;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.OpenModalDialog;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
import org.jgrapes.webconsole.rbac.UserAuthenticated;
import org.jgrapes.webconsole.rbac.UserLoggedOut;

/**
 * A login conlet for OIDC based logins with a fallback to password 
 * based logins.
 * 
 * OIDC providers can be configured as property "oidcProviders" of the conlet:
 * ```yaml
 * "...":
 *   "/LoginConlet":
 *     oidcProviders:
 *     - name: my-provider
 *       displayName: My Provider
 *       configurationEndpoint: https://test.com/.well-known/openid-configuration
 *       # If no configurationEndpoint is available, the authorization 
 *       # endpointEndpoint and the tokenEndpoint may be configured instead
 *       clientId: "WebConsoleTest"
 *       secret: "(unknown)"
 *         # The size of the popup window for the provider's dialog
 *         popup:
 *           # Either as size reletive to the browser window or in pixels
 *           # factor: 0.6
 *           width: 1600
 *           height: 600
 * ```
 * 
 * The user id of the authenticated user is taken from the ID token's
 * claim `preferred_username`, the display name from the claim `name`.
 * Roles are created from the ID token's claim `roles`. The latter
 * has usually to be added in the provider's configuration. Of course,
 * roles can also be configured independently based on the user id
 * by using another component such as the {@link RoleConfigurator}.
 * 
 * The component requires that an instance of {@link OidcClient}
 * handles the {@link StartOidcClient} events fired on the component's
 * channel.
 * 
 * As a fallback, local users can be configured as property "users":
 * ```yaml
 * "...":
 *   "/LoginConlet":
 *     users:
 *     - name: admin
 *       # Full name is optional
 *       fullName: Administrator
 *       password: "$2b$05$NiBd74ZGdplLC63ePZf1f.UtjMKkbQ23cQoO2OKOFalDBHWAOy21."
 *     - name: test
 *       fullName: Test Account
 *       password: "$2b$05$hZaI/jToXf/d3BctZdT38Or7H7h6Pn2W3WiB49p5AyhDHFkkYCvo2"
 * ```
 * 
 * Passwords are hashed using bcrypt.
 * 
 * The local login part of the dialog is only shown if at least one user is
 * configured.
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.CouplingBetweenObjects",
    "PMD.ExcessiveImports" })
public class LoginConlet extends FreeMarkerConlet<LoginConlet.AccountModel> {

    private static final String PENDING_CONSOLE_PREPARED
        = "pendingConsolePrepared";
    private final Map<String, OidcProviderData> providers
        = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> users
        = new ConcurrentHashMap<>();

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public LoginConlet(Channel componentChannel) {
        super(componentChannel);
    }

    @Override
    protected String generateInstanceId(AddConletRequest event,
            ConsoleConnection session) {
        return "Singleton";
    }

    /**
     * Register conlet.
     *
     * @param event the event
     * @param channel the channel
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleConnection channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Define providers
        StringBuffer script = new StringBuffer(1000);
        script.append("window.orgJGrapesOidcLogin"
            + " = window.orgJGrapesOidcLogin || {};"
            + "window.orgJGrapesOidcLogin.providers = [];");
        boolean first = true;
        for (var e : providers.entrySet()) {
            if (first) {
                script.append("let ");
                first = false;
            }
            script.append("provider = new Map();"
                + "window.orgJGrapesOidcLogin.providers.push(provider);"
                + "provider.set('name', '").append(e.getKey())
                .append("');provider.set('displayName', '")
                .append(e.getValue().displayName()).append("');");
            if (e.getValue().popup() != null) {
                for (var d : e.getValue().popup().entrySet()) {
                    String key = "popup" + d.getKey().substring(0, 1)
                        .toUpperCase() + d.getKey().substring(1);
                    script.append("provider.set('").append(key).append("', '")
                        .append(d.getValue()).append("');");
                }
            }
        }
        var providerDefs = script.toString();
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "Login-functions.js"))
                .setScriptType("module"))
            .addScript(new ScriptResource().setScriptSource(providerDefs))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "Login-style.css"))
            .addPageContent("headerIcons", Map.of("priority", "1000"))
            .addRenderMode(RenderMode.Content));
    }

    /**
     * As a model has already been created in {@link #doUpdateConletState},
     * the "new" model may already exist in the session.
     */
    @Override
    protected Optional<AccountModel> createNewState(AddConletRequest event,
            ConsoleConnection session, String conletId) throws Exception {
        Optional<AccountModel> model
            = stateFromSession(session.session(), conletId);
        if (model.isPresent()) {
            return model;
        }
        return super.createNewState(event, session, conletId);
    }

    @Override
    protected Optional<AccountModel> createStateRepresentation(Event<?> event,
            ConsoleConnection channel, String conletId) throws IOException {
        return Optional.of(new AccountModel(conletId));
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `users`
     * : See {@link LoginConlet}.
     * 
     * @param event the event
     */
    @SuppressWarnings("unchecked")
    @Handler
    public void onConfigUpdate(ConfigurationUpdate event) {
        event.structured(componentPath())
            .map(c -> (List<Map<String, String>>) c.get("users"))
            .orElseGet(Collections::emptyList).stream()
            .forEach(e -> {
                var user = users.computeIfAbsent(e.get("name"),
                    k -> new ConcurrentHashMap<>());
                user.putAll(e);
            });
        ObjectMapper mapper = new ObjectMapper();
        event.structured(componentPath())
            .map(c -> (List<Map<String, String>>) c.get("oidcProviders"))
            .orElseGet(Collections::emptyList).stream()
            .forEach(e -> {
                var data = mapper.convertValue(e, OidcProviderData.class);
                providers.put(data.name(), data);
            });
    }

    /**
     * Handle web console page loaded.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException 
     * @throws ParseException 
     * @throws MalformedTemplateNameException 
     * @throws TemplateNotFoundException 
     */
    @Handler(priority = 1000)
    public void onConsolePrepared(ConsolePrepared event,
            ConsoleConnection channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // If we are logged in, proceed
        if (channel.session().containsKey(Subject.class)) {
            return;
        }

        // Suspend handling and save event "in" channel.
        event.suspendHandling();
        channel.setAssociated(PENDING_CONSOLE_PREPARED, event);

        // Create model and save in session.
        String conletId = type() + TYPE_INSTANCE_SEPARATOR + "Singleton";
        AccountModel accountModel = new AccountModel(conletId);
        accountModel.setDialogOpen(true);
        putInSession(channel.session(), conletId, accountModel);

        // Render login dialog
        var fmModel = fmSessionModel(channel.session());
        fmModel.put("hasUser", !users.isEmpty());
        fmModel.put("providers", providers);
        Template tpl = freemarkerConfig().getTemplate("Login-dialog.ftl.html");
        var bundle = resourceBundle(channel.locale());
        channel.respond(new OpenModalDialog(type(), conletId,
            processTemplate(event, tpl, fmModel))
                .addOption("title", bundle.getString("title"))
                .addOption("cancelable", false)
                .addOption("applyLabel",
                    providers.isEmpty() ? bundle.getString("Log in") : "")
                .addOption("useSubmit", true));
    }

    private Future<String> processTemplate(
            Event<?> request, Template template,
            Object dataModel) {
        return request.processedBy().map(EventPipeline::executorService)
            .orElse(Components.defaultExecutorService()).submit(() -> {
                StringWriter out = new StringWriter();
                try {
                    template.process(dataModel, out);
                } catch (TemplateException | IOException e) {
                    throw new IllegalArgumentException(e);
                }
                return out.toString();

            });
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            AccountModel model) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Content)) {
            Template tpl
                = freemarkerConfig().getTemplate("Login-status.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, model)))
                        .setRenderAs(RenderMode.Content));
            channel.respond(new NotifyConletView(type(), conletId,
                "updateUser",
                WebConsoleUtils.userFromSession(channel.session())
                    .map(ConsoleUser::getDisplayName).orElse(null)));
            renderedAs.add(RenderMode.Content);
        }
        return renderedAs;
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidLiteralsInIfCondition", "PMD.AvoidLiteralsInIfCondition" })
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection connection, AccountModel model) throws Exception {
        if ("loginData".equals(event.method())) {
            attemptLocalLogin(event, connection, model);
            return;
        }
        if ("useProvider".equals(event.method())) {
            var locales = Optional.ofNullable(
                (Selection) connection.session().get(Selection.class))
                .map(Selection::get).orElse(new Locale[0]);
            fire(new StartOidcLogin(providers.get(event.params().asString(0)),
                locales).setAssociated(this,
                    new LoginContext(connection, model)));
            return;
        }
        if ("logout".equals(event.method())) {
            Optional.ofNullable((Subject) connection.session()
                .get(Subject.class)).map(UserLoggedOut::new).map(this::fire);
            connection.responsePipeline()
                .fire(new Close(), connection.upstreamChannel()).get();
            connection.close();
            connection.respond(new DiscardSession(connection.session(),
                connection.webletChannel()));
            // Alternative to sending Close (see above):
            // channel.respond(new SimpleConsoleCommand("reload"));
        }
    }

    private void attemptLocalLogin(NotifyConletModel event,
            ConsoleConnection connection, AccountModel model) {
        var bundle = resourceBundle(connection.locale());
        String userName = event.params().asString(0);
        if (userName == null || userName.isEmpty()) {
            connection.respond(new NotifyConletView(type(),
                model.getConletId(), "setMessages",
                null, bundle.getString("emptyUserName")));
            return;
        }
        var userData = users.get(userName);
        String password = event.params().asString(1);
        if (userData == null
            || !BCrypt.verifyer().verify(password.getBytes(),
                userData.get("password").getBytes()).verified) {
            connection.respond(new NotifyConletView(type(),
                model.getConletId(), "setMessages",
                null, bundle.getString("invalidCredentials")));
            return;
        }
        Subject subject = new Subject();
        subject.getPrincipals().add(new ConsoleUser(userName,
            Optional.ofNullable(userData.get("fullName"))
                .orElse(userName)));
        fire(new UserAuthenticated(event.setAssociated(this,
            new LoginContext(connection, model)), subject).by("Local Login"));
    }

    /**
     * Invoked when the OIDC client has assembled the required information
     * for contacting the provider.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onOpenLoginWindow(OpenLoginWindow event, Channel channel) {
        event.forLogin().associated(this, LoginContext.class)
            .filter(ctx -> ctx.conlet() == this).ifPresent(ctx -> {
                ctx.connection.respond(new NotifyConletView(type(),
                    ctx.model.getConletId(), "openLoginWindow",
                    event.uri().toString()));
            });
    }

    /**
     * Invoked when a user has been authenticated.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onUserAuthenticated(UserAuthenticated event, Channel channel) {
        var ctx = event.forLogin().associated(this, LoginContext.class)
            .filter(c -> c.conlet() == this).orElse(null);
        if (ctx == null) {
            return;
        }
        var model = ctx.model;
        model.setDialogOpen(false);
        var connection = ctx.connection;
        connection.session().put(Subject.class, event.subject());
        connection.respond(new CloseModalDialog(type(), model.getConletId()));
        connection
            .associated(PENDING_CONSOLE_PREPARED, ConsolePrepared.class)
            .ifPresentOrElse(ConsolePrepared::resumeHandling,
                () -> connection
                    .respond(new SimpleConsoleCommand("reload")));
    }

    /**
     * Do set locale.
     *
     * @param event the event
     * @param channel the channel
     * @param model the conlet id
     * @return true, if successful
     * @throws Exception the exception
     */
    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleConnection channel,
            String conletId) throws Exception {
        return stateFromSession(channel.session(),
            type() + TYPE_INSTANCE_SEPARATOR + "Singleton")
                .map(model -> !model.isDialogOpen()).orElse(true);
    }

    /**
     * The context to preserve during the authentication process.
     */
    private class LoginContext {
        public final ConsoleConnection connection;
        public final AccountModel model;

        /**
         * Instantiates a new oidc context.
         *
         * @param connection the connection
         * @param model the model
         */
        public LoginContext(ConsoleConnection connection, AccountModel model) {
            this.connection = connection;
            this.model = model;
        }

        /**
         * Returns the conlet (the outer class).
         *
         * @return the login conlet
         */
        public LoginConlet conlet() {
            return LoginConlet.this;
        }
    }

    /**
     * Model with account info.
     */
    public static class AccountModel extends ConletBaseModel {

        private boolean dialogOpen;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param model the web console component id
         */
        @ConstructorProperties({ "model" })
        public AccountModel(String conletId) {
            super(conletId);
        }

        /**
         * Checks if is dialog open.
         *
         * @return true, if is dialog open
         */
        public boolean isDialogOpen() {
            return dialogOpen;
        }

        /**
         * Sets the dialog open.
         *
         * @param dialogOpen the new dialog open
         */
        public void setDialogOpen(boolean dialogOpen) {
            this.dialogOpen = dialogOpen;
        }
    }

}
