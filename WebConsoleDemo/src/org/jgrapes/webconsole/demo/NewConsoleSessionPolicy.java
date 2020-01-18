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

package org.jgrapes.webconsole.demo;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.events.ConsoleConfigured;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.RenderConlet;

/**
 * 
 */
public class NewConsoleSessionPolicy extends Component {

    private final String renderedFlagName = getClass().getName() + ".rendered";

    /**
     * Creates a new component with its channel set to
     * itself.
     */
    public NewConsoleSessionPolicy() {
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel
     */
    public NewConsoleSessionPolicy(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onPortalReady(ConsoleReady event, ConsoleSession portalsession) {
        portalsession.browserSession().put(renderedFlagName, false);
    }

    @Handler
    public void onRenderPortlet(RenderConlet event,
            ConsoleSession portalsession) {
        portalsession.browserSession().put(renderedFlagName, true);
    }

    @Handler
    public void onPortalConfigured(ConsoleConfigured event,
            ConsoleSession portalSession)
            throws InterruptedException {
        if ((Boolean) portalSession.browserSession().getOrDefault(
            renderedFlagName, false)) {
            return;
        }
//        fire(new AddConletRequest(event.event().event().renderSupport(),
//            MarkdownDisplayConlet.class.getName(),
//            Conlet.RenderMode.Preview)
//                .addProperty(MarkdownDisplayConlet.TITLE, "Demo WebConsole")
//                .addProperty(MarkdownDisplayConlet.PREVIEW_SOURCE,
//                    "A Demo WebConsole")
//                .addProperty(MarkdownDisplayConlet.EDITABLE_BY,
//                    Collections.EMPTY_SET),
//            portalSession);
    }

}
