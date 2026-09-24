/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
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

package org.jgrapes.webconsole.alpinejs;

import freemarker.template.Configuration;
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
 * 
 * The weblet sets the tag interpolation syntax of freemarker to
 * bracket tag syntax (`[= ...]`) to distinguish freemarker
 * interpolation from Alpine interpolation.
 */
public class AlpineJsConsoleWeblet extends FreeMarkerConsoleWeblet {

    /**
     * Instantiates a new lit based weblet.
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the console channel
     * @param consolePrefix the console prefix
     */
    public AlpineJsConsoleWeblet(Channel webletChannel, Channel consoleChannel,
            URI consolePrefix) {
        super(webletChannel, consoleChannel, consolePrefix);
//        freeMarkerConfig.setInterpolationSyntax(
//            Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
    }

    @Override
    public String styling() {
        return "markupFirst";
    }
}
