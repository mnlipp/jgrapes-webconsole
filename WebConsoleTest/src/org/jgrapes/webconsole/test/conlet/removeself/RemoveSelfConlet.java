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

package org.jgrapes.webconsole.test.conlet.removeself;

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
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.UpdateConletType;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * A conlet that provides a button that removes its own type from the
 * console. Used for testing.
 */
public class RemoveSelfConlet extends FreeMarkerConlet<Serializable> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview);
    private boolean deleteConlets;

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public RemoveSelfConlet(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On console ready.
     *
     * @param event the event
     * @param connection the console connection
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleConnection connection)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add conlet resources to page
        connection.respond(new AddConletType(type())
            .addRenderMode(RenderMode.Preview).setDisplayNames(
                localizations(connection.supportedLocales(), "conletName"))
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().conletResource(type(),
                    "RemoveSelf-functions.js")))
            .addScript(new ScriptResource().setScriptId("testsource")
                .setScriptType("text/x-test")
                .setScriptSource("Just a test.")));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            Serializable conletState)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("RemoveSelf-preview.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.Preview.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        return renderedAs;
    }

    @Override
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection channel, Serializable conletState)
            throws Exception {
        event.stop();
        deleteConlets = event.params().asBoolean(1);
        detach();
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected void doRemoveConletType() {
        if (deleteConlets) {
            super.doRemoveConletType();
            return;
        }
        for (ConsoleConnection session : trackedConnections()) {
            session.respond(new UpdateConletType(type()));
        }
    }

}
