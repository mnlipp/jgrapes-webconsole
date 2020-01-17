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

package org.jgrapes.webcon.provider.vuex;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webcon.base.PageResourceProvider;
import org.jgrapes.webcon.base.PortalSession;
import org.jgrapes.webcon.base.events.AddPageResources;
import org.jgrapes.webcon.base.events.PortalReady;
import org.jgrapes.webcon.base.events.AddPageResources.ScriptResource;

/**
 * Provider for the [Vuex](https://vuex.vuejs.org/guide/) library.
 */
public class VuexProvider extends PageResourceProvider {

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public VuexProvider(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On {@link PortalReady}, fire the appropriate {@link AddPageResources}.
     *
     * @param event the event
     * @param portalSession the portal session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(priority = 100)
    public void onPortalReady(PortalReady event, PortalSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        portalSession.respond(new AddPageResources()
            .addScriptResource(new ScriptResource()
                .setProvides(new String[] { "vuex" })));
    }
}
