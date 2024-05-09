/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

import java.util.Set;
import org.jgrapes.core.Event;
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.ConsoleUser;

/**
 * The Class OidcUserAuthenticated.
 */
public class OidcUserAuthenticated extends Event<Void> {

    private final StartOidcLogin forLogin;
    private final ConsoleUser user;
    private final Set<ConsoleRole> roles;

    /**
     * Instantiates a new oidc user authenticated.
     *
     * @param forLogin the for login
     * @param user the user
     * @param roles the roles
     */
    public OidcUserAuthenticated(StartOidcLogin forLogin, ConsoleUser user,
            Set<ConsoleRole> roles) {
        this.forLogin = forLogin;
        this.user = user;
        this.roles = roles;
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
     * Gets the user.
     *
     * @return the user
     */
    public ConsoleUser user() {
        return user;
    }

    /**
     * Gets the roles.
     *
     * @return the roles
     */
    public Set<ConsoleRole> roles() {
        return roles;
    }

}
