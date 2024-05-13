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

package org.jgrapes.webconsole.rbac;

import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.ConsoleUser;
import org.jgrapes.webconsole.base.events.UserAuthenticated;
import org.jgrapes.webconsole.base.events.UserLoggedOut;

/**
 * A component that writes user authentication and log out events to the log.
 */
public class UserLogger extends Component {

    /**
     * Instantiates a new user logger.
     *
     * @param componentChannel the component channel
     */
    public UserLogger(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * User authenticated.
     *
     * @param event the event
     */
    @Handler
    public void userAuthenticated(UserAuthenticated event) {
        var user = event.subject().getPrincipals(ConsoleUser.class).stream()
            .findFirst().map(ConsoleUser::toString).orElse("(no user)");
        var roles = event.subject().getPrincipals(ConsoleRole.class).stream()
            .map(ConsoleRole::toString).collect(Collectors.joining(", "));
        logger.info(() -> "User " + user + " authenticated with roles " + roles
            + " by " + event.by().stream().collect(Collectors.joining(", ")));
    }

    /**
     * User logged out.
     *
     * @param event the event
     */
    @Handler
    public void userLoggedOut(UserLoggedOut event) {
        var user = event.subject().getPrincipals(ConsoleUser.class).stream()
            .findFirst().map(ConsoleUser::toString).orElse("(no user)");
        logger.info(() -> "User " + user + " logged out");
    }

}
