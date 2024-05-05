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

package org.jgrapes.webconlet.oidclogin;

import java.net.URI;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * The Class OpenLoginWindow.
 */
public class OpenLoginWindow extends Event<Void> {

    private final StartOidcLogin forLogin;
    private final URI uri;

    /**
     * Instantiates a new open login window.
     *
     * @param forLogin the initial {@link StartOidcLogin} event
     * @param uri the uri
     */
    public OpenLoginWindow(StartOidcLogin forLogin, URI uri) {
        this.forLogin = forLogin;
        this.uri = uri;
    }

    /**
     * Gets the initial {@link StartOidcLogin} event.
     *
     * @return the for login
     */
    public StartOidcLogin forLogin() {
        return forLogin;
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public URI uri() {
        return uri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(Components.objectName(this))
            .append(" [provider=").append(forLogin().provider());
        if (channels() != null) {
            builder.append(", channels=").append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }

}
