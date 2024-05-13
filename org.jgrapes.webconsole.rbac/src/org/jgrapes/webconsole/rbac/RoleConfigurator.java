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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.Subject;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.ConsoleUser;
import org.jgrapes.webconsole.base.events.UserAuthenticated;

/**
 * Configures roles (of type {@link ConsoleRole)} 
 * for the user currently logged in.
 */
public class RoleConfigurator extends Component {

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Set<String>> roles = new HashMap<>();
    private boolean replace;

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public RoleConfigurator(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     *
     * Supported properties are:
     * 
     *  * *rolesByUser*: see {@link #setRolesByUser(Map)}.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param properties the properties used to configure the component
     */
    @SuppressWarnings({ "unchecked", "PMD.ConstructorCallsOverridableMethod" })
    public RoleConfigurator(Channel componentChannel,
            Map<?, ?> properties) {
        super(componentChannel);
        setRolesByUser((Map<String, Set<String>>) properties
            .get("rolesByUser"));
    }

    /**
     * Sets the roles associated with a user. The parameter
     * is a Map<String, Set<String>> holding the roles to be  
     * associated with a given user. The special key "*" may
     * be used to specify roles that are to be added to any user.
     *
     * @param roles the roles
     * @return the user role conlet filter
     */
    @SuppressWarnings({ "PMD.LinguisticNaming",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public RoleConfigurator setRolesByUser(Map<String, Set<String>> roles) {
        this.roles.clear();
        this.roles.putAll(roles);
        for (var e : this.roles.entrySet()) {
            e.setValue(new HashSet<>(e.getValue()));
        }
        return this;
    }

    /**
     * Control whether the component replaces all {@link ConsoleRole}s
     * or adds to the existing roles (default).
     *
     * @param replace the replace
     * @return the role configurator
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public RoleConfigurator setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `rolesByUser`
     * : Invokes {@link #setRolesByUser(Map)} with the given values.
     *
     * `replace`
     * : Invokes {@link #setReplace(boolean)} with the given value.
     * 
     * @param event the event
     */
    @SuppressWarnings("unchecked")
    @Handler
    public void onConfigUpdate(ConfigurationUpdate event) {
        event.structured(componentPath())
            .map(c -> (Map<String, Collection<String>>) c.get("rolesByUser"))
            .map(m -> m.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                    e -> e.getValue().stream().collect(Collectors.toSet()))))
            .ifPresent(this::setRolesByUser);
        event.value(componentPath(), "replace").map(Boolean::valueOf)
            .ifPresent(this::setReplace);
    }

    /**
     * Sets the roles in the subject with the authenticated user.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 900)
    public void onUserAuthenticated(UserAuthenticated event, Channel channel) {
        Subject subject = event.subject();
        if (replace) {
            for (var itr = subject.getPrincipals().iterator(); itr.hasNext();) {
                if (itr.next() instanceof ConsoleRole) {
                    itr.remove();
                }
            }
        }
        Stream.concat(
            subject.getPrincipals(ConsoleUser.class).stream()
                .findFirst().map(ConsoleUser::getName).stream(),
            Stream.of("*")).map(roles::get).filter(Objects::nonNull)
            .flatMap(Set::stream).map(ConsoleRole::new)
            .forEach(p -> subject.getPrincipals().add(p));
        event.by("Role Configurator");
    }
}
