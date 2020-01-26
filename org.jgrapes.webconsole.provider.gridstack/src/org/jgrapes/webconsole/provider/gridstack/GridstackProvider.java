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

package org.jgrapes.webconsole.provider.gridstack;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.PageResourceProvider;
import org.jgrapes.webconsole.base.events.AddPageResources;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;

/**
 * Provider for the [Gridstack.js](http://gridstackjs.com/) library.
 */
public class GridstackProvider extends PageResourceProvider {

    private static Logger LOG
        = Logger.getLogger(GridstackProvider.class.getName());

    public enum Configuration {
        CoreOnly, CoreWithJQueryUI, All
    }

    private Configuration configuration = Configuration.All;

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param properties the properties used to configure the component
     */
    public GridstackProvider(Channel componentChannel,
            Map<Object, Object> properties) {
        super(componentChannel);
        if (properties.containsKey("configuration")) {
            String config = (String) properties.get("configuration");
            try {
                configuration = Configuration.valueOf(config);
            } catch (IllegalArgumentException e) {
                LOG.severe("Illegal value for \"configuration\": " + config);
            }
        }
    }

    /**
     * On {@link ConsoleReady}, fire the appropriate {@link AddPageResources}.
     *
     * @param event the event
     * @param consoleSession the web console session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(priority = 100)
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        String minExt = event.renderSupport()
            .useMinifiedResources() ? ".min" : "";
        AddPageResources addRequest = new AddPageResources()
            .addCss(event.renderSupport().pageResource(
                "gridstack/gridstack" + minExt + ".css"));
        switch (configuration) {
        case CoreOnly:
            addRequest
                .addScriptResource(new ScriptResource()
                    .setRequires("jquery")
                    .setProvides("gridstack")
                    .setScriptUri(event.renderSupport().pageResource(
                        "gridstack/gridstack" + minExt + ".js")));
            break;
        case CoreWithJQueryUI:
            addRequest.addScriptResource(new ScriptResource()
                .setRequires("jquery", "jquery-ui")
                .setProvides("gridstack")
                .setScriptUri(event.renderSupport().pageResource(
                    "gridstack/gridstack" + minExt + ".js")));
            addRequest.addScriptResource(new ScriptResource()
                .setProvides("gridstack.jQueryUI")
                .setScriptUri(event.renderSupport().pageResource(
                    "gridstack/gridstack.jQueryUI" + minExt + ".js")));
            break;
        case All:
            addRequest.addScriptResource(new ScriptResource()
                .setRequires("jquery")
                .setProvides("gridstack", "gridstack.all")
                .setScriptUri(event.renderSupport().pageResource(
                    "gridstack/gridstack.all.js")));
        }
        consoleSession.respond(addRequest);
    }
}
