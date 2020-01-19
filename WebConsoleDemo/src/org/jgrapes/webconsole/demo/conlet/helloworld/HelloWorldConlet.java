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
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteConlet;
import org.jgrapes.webconsole.base.events.DeleteConletRequest;
import org.jgrapes.webconsole.base.events.DisplayNotification;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerComponent;
import org.jgrapes.webconsole.demo.conlet.helloworld.HelloWorldConlet;

/**
 * 
 */
public class HelloWorldConlet
        extends FreeMarkerComponent<HelloWorldConlet.HelloWorldModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.DeleteablePreview, RenderMode.View);

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
    public void onConsoleReady(ConsoleReady event, ConsoleSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        portalSession.respond(new AddConletType(type())
            .setDisplayNames(
                displayNames(portalSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().conletResource(type(),
                    "HelloWorld-functions.js")))
            .addScript(new ScriptResource().setScriptId("testsource")
                .setScriptType("text/x-test")
                .setScriptSource("Just a test."))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "HelloWorld-style.css")));
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(portalSession.browserSession()), portalSession);
        fire(query, portalSession);
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
    public String doAddConlet(AddConletRequest event,
            ConsoleSession channel) throws Exception {
        String conletId = generateConletId();
        HelloWorldModel conletModel = putInSession(
            channel.browserSession(), new HelloWorldModel(conletId));
        String jsonState = JsonBeanEncoder.create()
            .writeObject(conletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession()) + conletModel.getConletId(),
            jsonState));
        renderConlet(event, channel, conletModel);
        return conletId;
    }

    @Override
    protected void doRenderConlet(RenderConletRequest event,
            ConsoleSession channel, String conletId,
            HelloWorldModel conletModel) throws Exception {
        renderConlet(event, channel, conletModel);
    }

    private void renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, HelloWorldModel conletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        if (event.renderPreview()) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
            channel.respond(new RenderConletFromTemplate(event,
                HelloWorldConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderMode(RenderMode.Preview)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
        }
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
            channel.respond(new RenderConletFromTemplate(event,
                HelloWorldConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderMode(RenderMode.View)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "setWorldVisible",
                conletModel.isWorldVisible()));
        }
    }

    @Override
    protected void doDeleteConlet(DeleteConletRequest event,
            ConsoleSession channel, String conletId,
            HelloWorldModel conletState) throws Exception {
        channel.respond(new KeyValueStoreUpdate().delete(
            storagePath(channel.browserSession()) + conletId));
        channel.respond(new DeleteConlet(conletId));
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