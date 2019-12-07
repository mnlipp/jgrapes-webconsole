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

package org.jgrapes.portal.vuejs;

import java.net.URI;

import org.jgrapes.core.Channel;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.portal.base.Portal;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortalWeblet;

/**
 * Provides resources using {@link Request}/{@link Response}
 * events. Some resource requests (page resource, portlet resource)
 * are forwarded via the {@link Portal} component to the portlets.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.TooManyMethods" })
public class VueJsPortalWeblet extends FreeMarkerPortalWeblet {

    /**
     * Instantiates a new vue.js UI weblet.
     *
     * @param webletChannel the weblet channel
     * @param portalChannel the portal channel
     * @param portalPrefix the portal prefix
     */
    public VueJsPortalWeblet(Channel webletChannel, Channel portalChannel,
            URI portalPrefix) {
        super(webletChannel, portalChannel, portalPrefix);
    }

    @Override
    public String styling() {
        return "markupFirst";
    }
}
