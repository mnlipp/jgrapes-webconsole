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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.jgrapes.webconsole.base.ConsoleRole;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.UpdateConletType;

/**
 * Configures the conlets available based on the user currently logged in.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class RoleConletFilter extends Component {

    /**
     * The possible permissions.
     */
    private enum Permission {
        ADD, RENDER
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, List<String>> acl = new HashMap<>();
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
    @SuppressWarnings({ "PMD.ConstructorCallsOverridableMethod", "unchecked",
        "PMD.AvoidDuplicateLiterals" })
    public RoleConletFilter(Channel componentChannel,
            Map<?, ?> properties) {
        super(componentChannel);
        setConletTypesByRole((Map<String, List<String>>) properties
            .get("conletTypesByRole"));
    }

    /**
     * Sets the permitted conlet types by role. The parameter
     * is a Map<String, List<String>> holding the conlet permissions
     * to be added for the given role. The permissions can be an 
     * asterisk ("*") to allow usage of all conlet types. It can be
     * a conlet type as reported by {@link AddConletType#conletType()}
     * to allow the usage of a particular conlet. The type may be 
     * prefixed with an exclamation mark to deny the usage (default).
     *
     * The first match is used, i.e. the asterisk makes only sense
     * as the last element in the list.
     *
     * @param acl the acl
     * @return the user role conlet filter
     */
    @SuppressWarnings({ "PMD.LinguisticNaming",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public RoleConletFilter
            setConletTypesByRole(Map<String, List<String>> acl) {
        // Deep copy (and cleanup)
        this.acl.clear();
        this.acl.putAll(acl);
        for (var e : this.acl.entrySet()) {
            e.setValue(e.getValue().stream().map(String::trim)
                .collect(Collectors.toList()));
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
            .map(c -> (Map<String, List<String>>) c
                .get("conletTypesByRole"))
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
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AvoidLiteralsInIfCondition", "PMD.CognitiveComplexity" })
    public void onConsolePrepared(ConsolePrepared event,
            ConsoleConnection channel) {
        var permissions = new HashMap<String, Set<Permission>>();
        for (var conletType : knownTypes) {
            var conletPerms = new HashSet<Permission>();
            for (var role : WebConsoleUtils
                .rolesFromSession(channel.session())) {
                var perms = permissionsFromRole(conletType, role);
                logger.finest(() -> "Role " + role.getName() + " allows user "
                    + WebConsoleUtils.userFromSession(channel.session()).get()
                        .getName()
                    + " to " + perms + " " + conletType);
                conletPerms.addAll(perms);
            }
            permissions.put(conletType, conletPerms);
        }

        // Disable non-addable conlet types in GUI
        for (var e : permissions.entrySet()) {
            if (!e.getValue().contains(Permission.ADD)) {
                channel.respond(new UpdateConletType(e.getKey()));
            }
        }
        channel.setAssociated(this, permissions);
    }

    /**
     * Evaluate the permissions contributed by the given role for the
     * given conlet type.
     *
     * @param conletType the conlet type
     * @param role the role
     * @return the sets the
     */
    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private Set<Permission> permissionsFromRole(String conletType,
            ConsoleRole role) {
        // Start with defaults
        for (var rule : acl.getOrDefault(role.getName(),
            Collections.emptyList())) {
            // Extract conlet type
            int pos = 0;
            while (rule.charAt(pos) == '!' || rule.charAt(pos) == '-') {
                pos++;
            }
            if (rule.startsWith("*")) {
                return Set.of(Permission.ADD, Permission.RENDER);
            }
            if (!conletType.equals(rule.substring(pos).trim())) {
                // Rule does not apply to this type
                continue;
            }
            if (rule.startsWith("--")) {
                return Collections.emptySet();
            }
            if (rule.startsWith("!") || rule.startsWith("-")) {
                return Set.of(Permission.RENDER);
            }
            // Rule is type name and thus allows everything
            return Set.of(Permission.ADD, Permission.RENDER);
        }
        // Default permissions
        return Set.of(Permission.RENDER);
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
        channel.associated(this, Map.class).ifPresent(aP -> {
            @SuppressWarnings("unchecked")
            var allPerms = (Map<String, Set<Permission>>) aP;
            var perms = allPerms.getOrDefault(event.conletType(),
                Collections.emptySet());
            if (event.isFrontendRequest() ? !perms.contains(Permission.ADD)
                : !perms.contains(Permission.RENDER)) {
                event.cancel(true);
            }
        });
    }

    /**
     * If a role is withdrawn from a user, there may still be conlets in
     * his stored layout that he is no longer allowed to use. 
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 1000)
    public void onRenderConletRequest(RenderConletRequest event,
            ConsoleConnection channel) {
        channel.associated(this, Map.class).ifPresent(aP -> {
            @SuppressWarnings("unchecked")
            var allPerms = (Map<String, Set<Permission>>) aP;
            var perms = allPerms.getOrDefault(event.conletType(),
                Collections.emptySet());
            if (perms.isEmpty()) {
                event.cancel(true);
            }
        });
    }
}
