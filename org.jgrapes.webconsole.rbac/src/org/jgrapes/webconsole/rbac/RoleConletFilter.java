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

package org.jgrapes.webconsole.rbac;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.UpdateConletType;

/**
 * Configures the conlets available based on the user currently logged in.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class RoleConletFilter extends Component {

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Set<String>> acl = new HashMap<>();
    private final Set<String> knownTypes = new HashSet<>();

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public RoleConletFilter(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * Supported properties are:
     * 
     *  * *conletTypesByRole*: see {@link #setConletTypesByRole(Map)}.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param properties the properties used to configure the component
     */
    @SuppressWarnings({ "unchecked", "PMD.ConstructorCallsOverridableMethod" })
    public RoleConletFilter(Channel componentChannel,
            Map<?, ?> properties) {
        super(componentChannel);
        setConletTypesByRole((Map<String, Set<String>>) properties
            .get("conletTypesByRole"));
    }

    /**
     * Sets the permitted conlet types by role. The parameter
     * is a Map<String, Set<String>> holding the conlet types 
     * {@link AddConletType#conletType()} to be added for the given role.
     * Conlets that are not restricted to at least one role are 
     * implicitly available to all users.
     *
     * @param acl the acl
     * @return the user role conlet filter
     */
    @SuppressWarnings({ "PMD.LinguisticNaming",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public RoleConletFilter
            setConletTypesByRole(Map<String, Set<String>> acl) {
        // Deep copy (and cleanup)
        this.acl.clear();
        this.acl.putAll(acl);
        for (var e : this.acl.entrySet()) {
            e.setValue(e.getValue().stream().map(String::trim)
                .collect(Collectors.toSet()));
        }
        return this;
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `conletTypesByRole`
     * : Invokes {@link #setConletTypesByRole(Map)} with the
     *   given values.
     * 
     * @param event the event
     */
    @SuppressWarnings("unchecked")
    @Handler
    public void onConfigUpdate(ConfigurationUpdate event) {
        event.structured(componentPath())
            .map(c -> (Map<String, Collection<String>>) c
                .get("conletTypesByRole"))
            // Adjust type (Collection -> Set)
            .map(m -> m.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                    e -> e.getValue().stream().collect(Collectors.toSet()))))
            .ifPresent(this::setConletTypesByRole);
    }

    /**
     * Collect known types for wildcard handling
     *
     * @param event the event
     */
    @Handler
    public void onAddConletType(AddConletType event) {
        knownTypes.add(event.conletType());
    }

    /**
     * Disable all conlets that the user is not allowed to use
     * by firing {@link UpdateConletType} events with no render modes.
     * The current user is obtained from 
     * {@link WebConsoleUtils#userFromSession(org.jgrapes.http.Session)}.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 800)
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onConsolePrepared(ConsolePrepared event,
            ConsoleConnection channel) {
        var allowed = new HashSet<String>();
        WebConsoleUtils.rolesFromSession(channel.session()).forEach(
            role -> {
                var maybe = new HashSet<>(knownTypes);
                acl.getOrDefault(role.getName(), Collections.emptySet())
                    .forEach(p -> {
                        if (p.startsWith("!")) {
                            maybe.remove(p.substring(1).trim());
                        } else if ("*".equals(p)) {
                            allowed.addAll(maybe);
                        } else {
                            if (knownTypes.contains(p)) {
                                allowed.add(p);
                            }
                        }
                    });
            });
        channel.setAssociated(this, allowed);
        Set<String> toRemove = new HashSet<>(knownTypes);
        toRemove.removeAll(allowed);
        for (var type : toRemove) {
            channel.respond(new UpdateConletType(type));
        }
    }

    /**
     * If the request originates from a client 
     * (see {@link AddConletRequest#isFrontendRequest()}, verifies that
     * the user is allowed to create a conlet of the given type.
     * 
     * As the conlets that he user is not allowed to use are disabled,
     * it should be impossible to create such requests in the first place.
     * However, the frontend code is open to manipulation and therefore
     * this additional check is introduced to increase security.  
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 1000)
    public void onAddConlet(AddConletRequest event, ConsoleConnection channel) {
        if (!event.isFrontendRequest()) {
            return;
        }
        var allowed = channel.associated(this, Set.class);
        if (allowed.isEmpty() || !allowed.get().contains(event.conletType())) {
            event.cancel(true);
        }
    }
}
