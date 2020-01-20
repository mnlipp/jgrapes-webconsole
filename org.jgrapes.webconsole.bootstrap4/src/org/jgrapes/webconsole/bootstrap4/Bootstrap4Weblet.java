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

package org.jgrapes.webconsole.bootstrap4;

import java.net.URI;

import org.jgrapes.core.Channel;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.webconsole.base.WebConsole;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConsoleWeblet;

/**
 * Provides resources using {@link Request}/{@link Response}
 * events. Some resource requests (page resource, conlet resource)
 * are forwarded via the {@link WebConsole} component to the 
 * web console components.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public class Bootstrap4Weblet extends FreeMarkerConsoleWeblet {

    /**
     * Instantiates a new Bootstrap 3 UI weblet.
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the web console channel
     * @param consolePrefix the web console prefix
     */
    public Bootstrap4Weblet(Channel webletChannel, Channel consoleChannel,
            URI consolePrefix) {
        super(webletChannel, consoleChannel, consolePrefix);
    }

    @Override
    public String styling() {
        return "bootstrap4";
    }

}
