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

package org.jgrapes.webconlet.examples.login;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import javax.security.auth.Subject;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.DiscardSession;
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

/**
 * A conlet for poll administration.
 */
public class LoginConlet extends FreeMarkerConlet<LoginConlet.AccountModel> {

    private static final String PENDING_CONSOLE_PREPARED
        = "pendingCosolePrepared";

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
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "Login-functions.js"))
                .setScriptType("module"))
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
    protected Optional<AccountModel> createStateRepresentation(
            RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId) throws IOException {
        return Optional.of(new AccountModel(conletId));
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
        Template tpl = freemarkerConfig().getTemplate("Login-dialog.ftl.html");
        var bundle = resourceBundle(channel.locale());
        channel.respond(new OpenModalDialog(type(), conletId,
            processTemplate(event, tpl,
                fmSessionModel(channel.session())))
                    .addOption("title", bundle.getString("title"))
                    .addOption("cancelable", false).addOption("okayLabel", "")
                    .addOption("applyLabel", bundle.getString("Submit"))
                    .addOption("useSubmit", true));
    }

    private Future<String> processTemplate(
            Event<?> request, Template template,
            Object dataModel) {
        return request.processedBy().map(procBy -> procBy.executorService())
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
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection channel, AccountModel model) throws Exception {
        if ("loginData".equals(event.method())) {
            model.setDialogOpen(false);
            Subject user = new Subject();
            user.getPrincipals().add(new ConsoleUser(event.params().asString(0),
                event.params().asString(0)));
            channel.session().put(Subject.class, user);
            channel.respond(new CloseModalDialog(type(), event.conletId()));
            channel.associated(PENDING_CONSOLE_PREPARED, ConsolePrepared.class)
                .ifPresentOrElse(ConsolePrepared::resumeHandling,
                    () -> channel.respond(new SimpleConsoleCommand("reload")));
            return;
        }
        if ("logout".equals(event.method())) {
            channel.respond(new DiscardSession(channel.session(),
                channel.webletChannel()));
            channel.respond(new SimpleConsoleCommand("reload"));
        }
    }

    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleConnection channel,
            String conletId) throws Exception {
        return stateFromSession(channel.session(),
            type() + TYPE_INSTANCE_SEPARATOR + "Singleton")
                .map(model -> !model.isDialogOpen()).orElse(true);
    }

    /**
     * Model with account info.
     */
    public static class AccountModel extends ConletBaseModel {

        private boolean dialogOpen;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param conletId the web console component id
         */
        @ConstructorProperties({ "conletId" })
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
