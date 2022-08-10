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

package org.jgrapes.webconlet.examples.helloworld;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConletBaseModel;
import org.jgrapes.webconsole.base.ConsoleSession;
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
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * Example of a simple conlet.
 */
public class HelloWorldConlet
        extends FreeMarkerConlet<HelloWorldConlet.HelloWorldModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View, RenderMode.Help);

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public HelloWorldConlet(Channel componentChannel) {
        super(componentChannel);
    }

    private String storagePath(Session session, String conletId) {
        return "/" + WebConsoleUtils.userFromSession(session)
            .map(ConsoleUser::getName).orElse("")
            + "/" + HelloWorldConlet.class.getName() + "/" + conletId;
    }

    /**
     * Trigger loading of resources when the console is ready.
     *
     * @param event the event
     * @param consoleSession the console session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .addRenderMode(RenderMode.Preview).setDisplayNames(
                localizations(consoleSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().conletResource(type(),
                    "HelloWorld-functions.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "HelloWorld-style.css")));
    }

    @Override
    protected Optional<HelloWorldModel> createStateRepresentation(
            RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId) throws IOException {
        HelloWorldModel conletModel = new HelloWorldModel(conletId);
        String jsonState
            = JsonBeanEncoder.create().writeObject(conletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession(), conletModel.getConletId()),
            jsonState));
        return Optional.of(conletModel);
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    protected Optional<HelloWorldModel> recreateState(
            RenderConletRequest event, ConsoleSession channel,
            String conletId) throws Exception {
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(channel.browserSession(), conletId), channel);
        newEventPipeline().fire(query, channel);
        try {
            if (!query.results().isEmpty()) {
                var json = query.results().get(0).values().stream().findFirst()
                    .get();
                HelloWorldModel model = JsonBeanDecoder.create(json)
                    .readObject(HelloWorldModel.class);
                return Optional.of(model);
            }
        } catch (InterruptedException | JsonDecodeException e) {
            // Means we have no result.
        }

        // Fall back to creating default state.
        return createStateRepresentation(event, channel, conletId);
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId,
            HelloWorldModel conletState) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-preview.ftl.html");
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
                = freemarkerConfig().getTemplate("HelloWorld-view.ftl.html");
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
                .getTemplate("HelloWorld-help.ftl.html");
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
            ConsoleSession channel, String conletId,
            HelloWorldModel conletState) throws Exception {
        if (event.renderModes().isEmpty()) {
            channel.respond(new KeyValueStoreUpdate().delete(
                storagePath(channel.browserSession(), conletId)));
        }
    }

    @Override
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleSession channel, HelloWorldModel conletModel)
            throws Exception {
        event.stop();
        conletModel.setWorldVisible(!conletModel.isWorldVisible());

        String jsonState = JsonBeanEncoder.create()
            .writeObject(conletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession(), conletModel.getConletId()),
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
    public static class HelloWorldModel extends ConletBaseModel {

        private boolean worldVisible = true;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param conletId the web console component id
         */
        @ConstructorProperties({ "conletId" })
        public HelloWorldModel(String conletId) {
            super(conletId);
        }

        /**
         * @param visible the visible to set
         */
        public void setWorldVisible(boolean visible) {
            this.worldVisible = visible;
        }

        public boolean isWorldVisible() {
            return worldVisible;
        }
    }

}
