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

import java.util.Locale;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * The Class StartOidcLogin.
 */
public class StartOidcLogin extends Event<Void> {

    private final OidcProviderData provider;
    private final Locale[] locales;

    /**
     * Instantiates a new event.
     *
     * @param provider the provider
     * @param locales the UI locales by preference
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public StartOidcLogin(OidcProviderData provider, Locale... locales) {
        this.provider = provider;
        this.locales = locales;
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    public OidcProviderData provider() {
        return provider;
    }

    /**
     * Gets the locales.
     *
     * @return the locales
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Locale[] locales() {
        return locales;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(30);
        builder.append(Components.objectName(this)).append(" [provider=")
            .append(provider.name());
        if (channels() != null) {
            builder.append(", channels=").append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }
}
