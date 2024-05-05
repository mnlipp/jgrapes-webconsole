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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class OidcProviderData.
 */
@SuppressWarnings("PMD.DataClass")
public class OidcProviderData {

    private final String name;
    private final String displayName;
    private URL configurationEndpoint;
    private URL authorizationEndpoint;
    private URL tokenEndpoint;
    private URL userinfoEndpoint;
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<String, String> popup = new HashMap<>();

    /**
     * Instantiates a new oidc provider data.
     *
     * @param name the name
     * @param displayName the display name
     * @param configurationEndpoint the configuration URL
     */
    public OidcProviderData(@JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName) {
        super();
        this.name = name;
        this.displayName = displayName != null ? displayName : name;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Gets the configuration endpoint.
     *
     * @return the configuration endpoint
     */
    public URL configurationEndpoint() {
        return configurationEndpoint;
    }

    /**
     * Sets the configuration endpoint.
     *
     * @param configurationEndpoint the new configuration endpoint
     */
    public void setConfigurationEndpoint(URL configurationEndpoint) {
        this.configurationEndpoint = configurationEndpoint;
    }

    /**
     * Gets the authorization endpoint.
     *
     * @return the authorization endpoint
     */
    public URL authorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * Sets the authorization endpoint.
     *
     * @param authorizationEndpoint the new authorization endpoint
     */
    public void setAuthorizationEndpoint(URL authorizationEp) {
        this.authorizationEndpoint = authorizationEp;
    }

    /**
     * Gets the token endpoint.
     *
     * @return the token endpoint
     */
    public URL tokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Sets the token endpoint.
     *
     * @param tokenEndpoint the new token endpoint
     */
    public void setTokenEndpoint(URL tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    /**
     * Gets the userinfo endpoint.
     *
     * @return the userinfo endpoint
     */
    public URL userinfoEndpoint() {
        return userinfoEndpoint;
    }

    /**
     * Sets the userinfo endpoint.
     *
     * @param userinfoEndpoint the new userinfo endpoint
     */
    public void setUserinfoEndpoint(URL userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    @Override
    public String toString() {
        return "Oidc Provider " + name + " (" + displayName
            + ") at " + configurationEndpoint;
    }

    /**
     * Gets the popup options.
     *
     * @return the popup
     */
    public Map<String, String> popup() {
        return popup;
    }

    /**
     * Sets the popup option.
     *
     * @param popup the popup
     */
    public void setPopup(Map<String, String> popup) {
        this.popup = popup;
    }

}
