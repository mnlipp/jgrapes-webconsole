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

package org.jgrapes.webconsole.base.freemarker;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Holds the information about a supported language. Used by
 * {@link FreeMarkerConsoleWeblet#expandConsoleModel(Map, 
 * org.jgrapes.http.events.Request.In.Get, UUID)} and
 * {@link FreeMarkerConlet#fmSessionModel(org.jgrapes.http.Session)}
 * to provide the function "supportedLanguages" during template
 * evaluation.
 */
public class LanguageInfo {
    private final Locale locale;
    private final ResourceBundle bundle;

    /**
     * Instantiates a new language info.
     *
     * @param locale the locale
     */
    public LanguageInfo(Locale locale, ResourceBundle bundle) {
        this.locale = locale;
        this.bundle = bundle;
    }

    /**
     * Gets the locale.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        String str = locale.getDisplayName(locale);
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Gets the bundle.
     *
     * @return the bundle
     */
    public ResourceBundle getL10nBundle() {
        return bundle;
    }
}