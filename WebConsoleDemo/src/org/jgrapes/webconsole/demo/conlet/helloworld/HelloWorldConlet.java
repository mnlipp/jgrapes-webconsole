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

package org.jgrapes.webconsole.demo.conlet.helloworld;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.AbstractConlet.ConletBaseModel;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.UserPrincipal;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConletDeleted;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DisplayNotification;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
import org.jgrapes.webconsole.demo.conlet.helloworld.HelloWorldConlet;

/**
 * 
 */
public class HelloWorldConlet
        extends FreeMarkerConlet<HelloWorldConlet.HelloWorldModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View);

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

    private String storagePath(Session session) {
        return "/" + WebConsoleUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/conlets/" + HelloWorldConlet.class.getName() + "/";
    }

    @Handler
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .setDisplayNames(
                displayNames(consoleSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().conletResource(type(),
                    "HelloWorld-functions.js")))
            .addScript(new ScriptResource().setScriptId("testsource")
                .setScriptType("text/x-test")
                .setScriptSource("Just a test."))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "HelloWorld-style.css")));
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(consoleSession.browserSession()), consoleSession);
        fire(query, consoleSession);
    }

    @Handler
    public void onKeyValueStoreData(
            KeyValueStoreData event, ConsoleSession channel)
            throws JsonDecodeException {
        if (!event.event().query()
            .equals(storagePath(channel.browserSession()))) {
            return;
        }
        for (String json : event.data().values()) {
            HelloWorldModel model = JsonBeanDecoder.create(json)
                .readObject(HelloWorldModel.class);
            putInSession(channel.browserSession(), model);
        }
    }

    @Override
    public ConletTrackingInfo doAddConlet(AddConletRequest event,
            ConsoleSession channel) throws Exception {
        String conletId = generateConletId();
        HelloWorldModel conletModel = putInSession(
            channel.browserSession(), new HelloWorldModel(conletId));
        String jsonState = JsonBeanEncoder.create()
            .writeObject(conletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession()) + conletModel.getConletId(),
            jsonState));
        return new ConletTrackingInfo(conletId)
            .addModes(renderConlet(event, channel, conletModel));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequest event,
            ConsoleSession channel, String conletId,
            HelloWorldModel conletModel) throws Exception {
        return renderConlet(event, channel, conletModel);
    }

    private Set<RenderMode> renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, HelloWorldModel conletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
            channel.respond(new RenderConletFromTemplate(event,
                HelloWorldConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
            channel.respond(new RenderConletFromTemplate(event,
                HelloWorldConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderAs(RenderMode.View.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "setWorldVisible",
                conletModel.isWorldVisible()));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    @Override
    protected void doConletDeleted(ConletDeleted event,
            ConsoleSession channel, String conletId,
            HelloWorldModel conletState) throws Exception {
        if (event.renderModes().isEmpty()) {
            channel.respond(new KeyValueStoreUpdate().delete(
                storagePath(channel.browserSession()) + conletId));
        }
    }

    @Override
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession channel, HelloWorldModel conletModel)
            throws Exception {
        event.stop();
        conletModel.setWorldVisible(!conletModel.isWorldVisible());

        String jsonState = JsonBeanEncoder.create()
            .writeObject(conletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession()) + conletModel.getConletId(),
            jsonState));
        channel.respond(new NotifyConletView(type(),
            conletModel.getConletId(), "setWorldVisible",
            conletModel.isWorldVisible()));
        channel.respond(new DisplayNotification("<span>"
            + resourceBundle(channel.locale()).getString("visibilityChange")
            + "</span>")
                .addOption("autoClose", 2000));
    }

    @SuppressWarnings("serial")
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
