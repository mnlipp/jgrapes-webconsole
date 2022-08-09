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

package org.jgrapes.webconsole.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.UpdateConletType;

/**
 * Configures the conlets available based on the user currently logged in.
 */
public class UserBasedConletFilter extends Component {

    private final Map<String, Set<String>> acl;
    private final Set<String> restricted;

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * Supported properties are:
     * 
     *  * *conletTypesByUsername*: a Map<String, Set<String>> holding
     *    the conlet types {@link AddConletType#conletType()} to be
     *    added for given user (see @link {@link ConsoleUser#getName()}.
     *    Conlets that are not restricted to at least one user are added
     *    for all users.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param properties the properties used to configure the component
     */
    @SuppressWarnings("unchecked")
    public UserBasedConletFilter(Channel componentChannel,
            Map<?, ?> properties) {
        super(componentChannel);
        acl = (Map<String, Set<String>>) properties
            .get("conletTypesByUsername");
        restricted = acl.values().stream().flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Filter the events.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 999)
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onConsolePrepared(ConsolePrepared event,
            ConsoleSession channel) {
        Set<String> toRemove = new HashSet<>(restricted);
        toRemove.removeAll(
            WebConsoleUtils.userFromSession(channel.browserSession())
                .map(ConsoleUser::getName).map(user -> acl.get(user))
                .orElse(Collections.emptySet()));
        for (var type : toRemove) {
            channel.respond(new UpdateConletType(type));
        }
    }
}
