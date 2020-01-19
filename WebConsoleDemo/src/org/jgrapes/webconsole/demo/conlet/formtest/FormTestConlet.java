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

package org.jgrapes.webconsole.demo.conlet.formtest;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

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
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerComponent;

/**
 * 
 */
public class FormTestConlet
        extends FreeMarkerComponent<ConletBaseModel> {

    private static final Set<RenderMode> MODES
        = RenderMode.asSet(RenderMode.View);

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public FormTestConlet(Channel componentChannel) {
        super(componentChannel);
    }

    private String storagePath(Session session) {
        return "/" + WebConsoleUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/conlets/" + FormTestConlet.class.getName() + "/";
    }

    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .setDisplayNames(
                displayNames(consoleSession.supportedLocales(), "conletName"))
            .addRenderMode(RenderMode.View)
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().conletResource(type(),
                    "FormTest-functions.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "FormTest-style.css")));
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
            ConletBaseModel model = JsonBeanDecoder.create(json)
                .readObject(ConletBaseModel.class);
            putInSession(channel.browserSession(), model);
        }
    }

    @Override
    public String doAddConlet(AddConletRequest event,
            ConsoleSession channel) throws Exception {
        String conletId = generateConletId();
        ConletBaseModel conletModel = putInSession(
            channel.browserSession(), new ConletBaseModel(conletId));
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
            ConletBaseModel conletModel) throws Exception {
        renderConlet(event, channel, conletModel);
    }

    private void renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, ConletBaseModel conletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("FormTest-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                FormTestConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderMode(RenderMode.View)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
        }
    }

    @Override
    protected void doDeleteConlet(DeleteConletRequest event,
            ConsoleSession channel, String conletId,
            ConletBaseModel conletState) throws Exception {
        channel.respond(new KeyValueStoreUpdate().delete(
            storagePath(channel.browserSession()) + conletId));
        channel.respond(new DeleteConlet(conletId));
    }

}
