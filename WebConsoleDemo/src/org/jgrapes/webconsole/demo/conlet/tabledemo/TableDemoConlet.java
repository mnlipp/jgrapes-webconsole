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
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * 
 */
public class TableDemoConlet extends FreeMarkerConlet<Serializable> {

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
    public TableDemoConlet(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add HelloWorldConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(consoleSession.supportedLocales(), "conletName"))
            .addRenderMode(RenderMode.View));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId,
            Serializable conletState) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-preview.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, channel, conletId, conletState))
                    .setRenderAs(event.renderAs())
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, channel, conletId, conletState))
                    .setRenderAs(RenderMode.View.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

}
