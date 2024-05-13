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

import javax.security.auth.Subject;
import org.jgrapes.core.Event;

// TODO: Auto-generated Javadoc
/**
 * The Class UserLoggedOut.
 */
public class UserLoggedOut extends Event<Void> {

    private final Subject subject;

    /**
     * Instantiates a new user logged out.
     *
     */
    public UserLoggedOut(Subject subject) {
        this.subject = subject;
    }

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public Subject subject() {
        return subject;
    }
}
