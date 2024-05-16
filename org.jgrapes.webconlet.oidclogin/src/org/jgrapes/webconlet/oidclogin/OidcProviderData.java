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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The Class OidcProviderData.
 */
@SuppressWarnings("PMD.DataClass")
public class OidcProviderData {

    private final String name;
    private final String displayName;
    private String clientId;
    private String secret;
    private URL issuer;
    private URL configurationEndpoint;
    private URL authorizationEndpoint;
    private URL tokenEndpoint;
    private URL userinfoEndpoint;
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<String, String> popup = new HashMap<>();
    private List<String> authorizedRoles = Collections.emptyList();
    private List<Map<String, String>> userMappings = Collections.emptyList();
    private List<Map<String, String>> roleMappings = Collections.emptyList();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Pattern> patternCache = new HashMap<>();

    /**
     * Instantiates a new oidc provider data.
     *
     * @param name the name
     * @param displayName the display name
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
     * Gets the client id.
     *
     * @return the client id
     */
    public String clientId() {
        return clientId;
    }

    /**
     * Sets the client id.
     *
     * @param clientId the new client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the secret.
     *
     * @return the secret
     */
    public String secret() {
        return secret;
    }

    /**
     * Sets the secret.
     *
     * @param secret the new secret
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Gets the issuer.
     *
     * @return the issuer
     */
    public URL issuer() {
        return issuer;
    }

    /**
     * Sets the issuer.
     *
     * @param issuer the new issuer
     */
    public void setIssuer(URL issuer) {
        this.issuer = issuer;
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
     * @param authorizationEp the new authorization endpoint
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

    /**
     * Gets the authorized roles.
     *
     * @return the authorized roles
     */
    public List<String> authorizedRoles() {
        return authorizedRoles;
    }

    /**
     * Sets the authorized roles.
     *
     * @param authorizedRoles the authorized roles
     */
    public void setAuthorizedRoles(List<String> authorizedRoles) {
        this.authorizedRoles = authorizedRoles;
    }

    /**
     * Gets the user mappings.
     *
     * @return the user mappings
     */
    public List<Map<String, String>> userMappings() {
        return userMappings;
    }

    /**
     * Sets the user mappings.
     *
     * @param userMappings the user mappings
     */
    public void setUserMappings(List<Map<String, String>> userMappings) {
        this.userMappings = userMappings;
    }

    /**
     * Gets the role mappings.
     *
     * @return the role mappings
     */
    public List<Map<String, String>> roleMappings() {
        return roleMappings;
    }

    /**
     * Sets the role mappings.
     *
     * @param roleMappings the role mappings
     */
    public void setRoleMappings(List<Map<String, String>> roleMappings) {
        this.roleMappings = roleMappings;
    }

    /**
     * Gets the pattern cache.
     *
     * @return the pattern cache
     */
    public Map<String, Pattern> patternCache() {
        return patternCache;
    }
}
