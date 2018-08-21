/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.portal.base;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * A {@link ResourceBundle.Control} that implements a special lookup
 * algorithm. See {@link #newBundle}. 
 */
public class PortalResourceBundleControl extends Control {

    private List<Class<?>> clses;

    public PortalResourceBundleControl(List<Class<?>> clses) {
        super();
        this.clses = clses;
    }

    public List<String> getFormats(String baseName) {
        if (baseName == null) {
            throw new NullPointerException();
        }
        return FORMAT_PROPERTIES;
    }

    /**
     * Returns `null` (no fallback).
     */
    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        return null;
    }

    /**
     * Creates a new resource bundle using the classes passed to the
     * constructor and the base name. For each class (in reverse order)
     * an attempt is made to load a properties file relative to the
     * class. If found, the entries are merged with any already existing
     * entries. The class list usually consists of the portal class
     * and its ancestor classes. Using this controller, derived classes
     * can thus override resources from their base classes.
     */
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale,
            String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        final String resourceName = toResourceName(bundleName, "properties");
        ListIterator<Class<?>> iter = clses.listIterator(clses.size());
        Properties props = new Properties();
        while (iter.hasPrevious()) {
            Class<?> cls = iter.previous();
            InputStream is = null;
            if (reload) {
                URL url = cls.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        // Disable caches to get fresh data for
                        // reloading.
                        connection.setUseCaches(false);
                        is = connection.getInputStream();
                    }
                }
            } else {
                is = cls.getResourceAsStream(resourceName);
            }
            if (is == null) {
                continue;
            }
            props.load(is);
            is.close();
            if (bundle == null) {
                bundle = new PortalResourceBundle(props);
            }
        }
        return bundle;
    }
}
