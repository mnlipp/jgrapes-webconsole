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

package org.jgrapes.webconsole.base.events;

import java.util.ArrayList;
import java.util.List;
import javax.security.auth.Subject;
import org.jgrapes.core.Event;

/**
 * The Class UserAuthenticated.
 */
public class UserAuthenticated extends Event<Void> {

    private final Event<?> forLogin;
    private final Subject subject;
    private final List<String> authenticators;

    /**
     * Instantiates successful user authentication.
     *
     * @param forLogin the for login
     */
    public UserAuthenticated(Event<?> forLogin, Subject subject) {
        this.forLogin = forLogin;
        this.subject = subject;
        authenticators = new ArrayList<>();
    }

    /**
     * Gets the event that initiated the login process.
     *
     * @return the for login
     */
    public Event<?> forLogin() {
        return forLogin;
    }

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public Subject subject() {
        return subject;
    }

    /**
     * Add the name of a component that has authenticated the user
     * or added roles.
     *
     * @param by the by
     * @return the user authenticated
     */
    @SuppressWarnings({ "PMD.ShortMethodName", "PMD.ShortVariable" })
    public UserAuthenticated by(String by) {
        authenticators.add(by);
        return this;
    }

    /**
     * Return the names of the components that have authenticated the user
     * or added roles.
     *
     * @return the list
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public List<String> by() {
        return authenticators;
    }
}
