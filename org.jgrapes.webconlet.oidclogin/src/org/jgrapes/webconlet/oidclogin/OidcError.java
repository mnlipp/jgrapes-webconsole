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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.events.Error;

/**
 * The Class OidcError.
 */
@SuppressWarnings("PMD.DataClass")
public class OidcError extends Error {

    /**
     * The error kind.
     */
    @SuppressWarnings("PMD.LongVariable")
    public enum Kind {
        INVALID_ISSUER, INVALID_AUDIENCE, ID_TOKEN_EXPIRED,
        PREFERRED_USERNAME_MISSING, ACCESS_DENIED
    }

    private final Kind kind;

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param kind the kind
     */
    public OidcError(Error event, Kind kind) {
        super(event);
        this.kind = kind;
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param kind the kind
     * @param message the message
     * @param throwable the throwable
     */
    public OidcError(Event<?> event, Kind kind, String message,
            Throwable throwable) {
        super(event, message, throwable);
        this.kind = kind;
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param kind the kind
     * @param message the message
     */
    public OidcError(Event<?> event, Kind kind, String message) {
        super(event, message);
        this.kind = kind;
    }

    /**
     * Instantiates a new oidc authentication failure.
     *
     * @param event the event
     * @param kind the kind
     * @param throwable the throwable
     */
    public OidcError(Event<?> event, Kind kind, Throwable throwable) {
        super(event, throwable);
        this.kind = kind;
    }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public Kind kind() {
        return kind;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(Components.objectName(this)).append(" [")
            .append(kind()).append(" (\"").append(message()).append("\")");
        if (channels().length > 0) {
            builder.append(", channels=").append(Channel.toString(channels()));
        }
        if (event() != null) {
            builder.append(", caused by: ").append(event().toString());
        }
        builder.append(']');
        return builder.toString();
    }
}
