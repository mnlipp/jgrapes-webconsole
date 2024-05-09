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

import org.jgrapes.core.Event;
import org.jgrapes.core.events.Error;

/**
 * The Class OidcError.
 */
@SuppressWarnings("PMD.DataClass")
public class OidcError extends Error {

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     */
    public OidcError(Error event) {
        super(event);
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param message the message
     * @param throwable the throwable
     */
    public OidcError(Event<?> event, String message,
            Throwable throwable) {
        super(event, message, throwable);
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param message the message
     */
    public OidcError(Event<?> event, String message) {
        super(event, message);
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param throwable the throwable
     */
    public OidcError(Event<?> event, Throwable throwable) {
        super(event, throwable);
    }

}
