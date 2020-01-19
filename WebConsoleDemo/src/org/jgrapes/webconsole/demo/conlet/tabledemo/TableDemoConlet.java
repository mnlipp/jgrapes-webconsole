/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.webconsole.demo.conlet.tabledemo;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.webconsole.base.AbstractConlet.ConletBaseModel;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteConlet;
import org.jgrapes.webconsole.base.events.DeleteConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerComponent;

/**
 * 
 */
public class TableDemoConlet extends FreeMarkerComponent<ConletBaseModel> {

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
    public TableDemoConlet(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        portalSession.respond(new AddConletType(type())
            .setDisplayNames(
                displayNames(portalSession.supportedLocales(), "conletName"))
            .addRenderMode(RenderMode.View));
    }

    @Override
    protected String generateConletId() {
        return type() + "-" + super.generateConletId();
    }

    @Override
    protected Optional<ConletBaseModel> stateFromSession(
            Session session, String conletId) {
        if (conletId.startsWith(type() + "-")) {
            return Optional.of(new ConletBaseModel(conletId));
        }
        return Optional.empty();
    }

    @Override
    public String doAddConlet(AddConletRequest event,
            ConsoleSession channel) throws Exception {
        String conletId = generateConletId();
        ConletBaseModel conletModel = putInSession(
            channel.browserSession(), new ConletBaseModel(conletId));
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
        if (event.renderPreview()) {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-preview.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                TableDemoConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderMode(RenderMode.Preview)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
        }
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                TableDemoConlet.class, conletModel.getConletId(),
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
        channel.respond(new DeleteConlet(conletId));
    }

}
