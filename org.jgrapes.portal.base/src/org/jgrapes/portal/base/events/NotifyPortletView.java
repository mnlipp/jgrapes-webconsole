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

package org.jgrapes.portal.base.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A notification (as defined by the JSON RPC specification) to be sent to
 * the portlet view (the browser).
 */
@SuppressWarnings("PMD.DataClass")
public class NotifyPortletView extends PortalCommand {

    private final String portletType;
    private final String portletId;
    private final String method;
    private final Object[] params;

    /**
     * Creates a new event.
     *  
     * @param portletType the portlet type (used by the portal 
     * core JS to look up the available functions, see {@link AddPortletType})
     * @param portletId the portlet (view) instance that the 
     * notification is directed at
     * @param method the method (function) to be executed, must
     * have been registered by handling {@link AddPortletType}
     * @param params the parameters
     */
    public NotifyPortletView(String portletType,
            String portletId, String method, Object... params) {
        this.portletType = portletType;
        this.portletId = portletId;
        this.method = method;
        this.params = Arrays.copyOf(params, params.length);
    }

    /**
     * Returns the portlet class.
     * 
     * @return the portlet class
     */
    public String portletType() {
        return portletType;
    }

    /**
     * Returns the portlet id.
     * 
     * @return the portlet id
     */
    public String portletId() {
        return portletId;
    }

    /**
     * Returns the method to be executed.
     * 
     * @return the method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the parameters.
     * 
     * @return the parameters
     */
    public Object[] params() {
        return Arrays.copyOf(params, params.length);
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        toJson(writer, "notifyPortletView", portletType(), portletId(),
            method(), params());
    }
}
