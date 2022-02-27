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

package org.jgrapes.webconlet.messagebox;

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
import org.jgrapes.webconsole.base.events.ConletDeleted;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * Example of a simple conlet.
 */
public class MessageBoxConlet extends FreeMarkerConlet<Serializable> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Component);

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public MessageBoxConlet(Channel componentChannel) {
        super(componentChannel);
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
        // Add resources to page
        consoleSession.respond(
            new AddConletType(type()).addRenderMode(RenderMode.Component));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId,
            Serializable conletState) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Component)) {
            Template tpl
                = freemarkerConfig().getTemplate("MessageBox.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, channel, conletId, conletState))
                    .setRenderAs(RenderMode.Component)
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Component);
        }
        return renderedAs;
    }

}
