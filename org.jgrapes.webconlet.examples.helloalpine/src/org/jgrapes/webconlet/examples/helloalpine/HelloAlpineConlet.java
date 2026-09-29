/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
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

package org.jgrapes.webconlet.examples.helloalpine;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConletBaseModel;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.ConsoleUser;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConletDeleted;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DisplayNotification;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.OpenModalDialog;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * Hello World example using AlpineJS for reactive UI.
 */
public class HelloAlpineConlet
        extends FreeMarkerConlet<HelloAlpineConlet.HelloAlpineModel> {

    /** The mapper. */
    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ObjectMapper mapper = new ObjectMapper();

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View, RenderMode.Help);

    /**
     * Instantiates a new hello alpine conlet.
     *
     * @param componentChannel the component channel
     */
    public HelloAlpineConlet(Channel componentChannel) {
        super(componentChannel);
    }

    private String storagePath(Session session, String conletId) {
        return "/" + WebConsoleUtils.userFromSession(session)
            .map(ConsoleUser::getName).orElse("")
            + "/" + HelloAlpineConlet.class.getName() + "/" + conletId;
    }

    /**
     * Called when the console is ready and the conlet can be registered.
     *
     * @param event the event
     * @param connection the connection
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleConnection connection)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        connection.respond(new AddConletType(type())
            .addRenderMode(RenderMode.Preview).setDisplayNames(
                localizations(connection.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setScriptType("module").setScriptUri(
                    event.renderSupport().conletResource(type(),
                        "HelloAlpine-functions.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "HelloAlpine-style.css")));
    }

    @Override
    protected Optional<HelloAlpineModel> createStateRepresentation(
            Event<?> event, ConsoleConnection channel, String conletId)
            throws IOException {
        HelloAlpineModel conletModel = new HelloAlpineModel(conletId);
        String jsonState = mapper.writer().writeValueAsString(conletModel);
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.session(), conletModel.getConletId()),
            jsonState));
        return Optional.of(conletModel);
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    protected Optional<HelloAlpineModel> recreateState(Event<?> event,
            ConsoleConnection channel, String conletId) throws Exception {
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(channel.session(), conletId), channel);
        newEventPipeline().fire(query, channel);
        try {
            if (!query.results().isEmpty()) {
                var json = query.results().get(0).values().stream().findFirst()
                    .get();
                HelloAlpineModel model = mapper.readValue(
                    json.getBytes(StandardCharsets.UTF_8),
                    HelloAlpineModel.class);
                return Optional.of(model);
            }
        } catch (InterruptedException | IOException e) {
            // Means we have no result.
        }
        return createStateRepresentation(event, channel, conletId);
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            HelloAlpineModel conletState) throws Exception {
        Set<RenderMode> renderedAs = EnumSet.noneOf(RenderMode.class);
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig()
                    .getTemplate("HelloAlpine-preview.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.Preview.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloAlpine-view.ftl.html");
            channel.respond(new RenderConlet(type(), conletState.getConletId(),
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.View.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            channel.respond(new NotifyConletView(type(),
                conletState.getConletId(), "setWorldVisible",
                conletState.isWorldVisible()));
            renderedAs.add(RenderMode.View);
        }
        if (event.renderAs().contains(RenderMode.Help)) {
            Template tpl = freemarkerConfig()
                .getTemplate("HelloAlpine-help.ftl.html");
            channel.respond(new OpenModalDialog(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .addOption("cancelable", true)
                        .addOption("closeLabel", ""));
        }
        return renderedAs;
    }

    @Override
    protected void doConletDeleted(ConletDeleted event,
            ConsoleConnection channel, String conletId,
            HelloAlpineModel conletState) throws Exception {
        if (event.renderModes().isEmpty()) {
            channel.respond(new KeyValueStoreUpdate().delete(
                storagePath(channel.session(), conletId)));
        }
    }

    @Override
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection channel, HelloAlpineModel conletModel)
            throws Exception {
        event.stop();
        conletModel.setWorldVisible(!conletModel.isWorldVisible());

        String jsonState = mapper.writer().writeValueAsString(conletModel);
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.session(), conletModel.getConletId()),
            jsonState));
        channel.respond(new NotifyConletView(type(),
            conletModel.getConletId(), "setWorldVisible",
            conletModel.isWorldVisible()));
        channel.respond(new DisplayNotification("<span>"
            + resourceBundle(channel.locale()).getString("visibilityChange")
            + "</span>")
                .addOption("autoClose", 2000));
    }

    /**
     * Model with world's state.
     */
    public static class HelloAlpineModel extends ConletBaseModel {

        private boolean worldVisible = true;

        /**
         * Instantiates a new hello alpine model.
         *
         * @param conletId the conlet id
         */
        @ConstructorProperties({ "conletId" })
        public HelloAlpineModel(String conletId) {
            super(conletId);
        }

        public void setWorldVisible(boolean visible) {
            this.worldVisible = visible;
        }

        public boolean isWorldVisible() {
            return worldVisible;
        }
    }
}
