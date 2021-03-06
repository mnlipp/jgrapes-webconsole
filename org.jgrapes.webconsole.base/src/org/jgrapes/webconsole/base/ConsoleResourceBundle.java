/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.webconsole.base;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Contains the result from {@link ConsoleResourceBundleControl#newBundle}.
 */
public class ConsoleResourceBundle extends ResourceBundle {

    private final Properties properties;

    /**
     * Instantiates a new web console resource bundle.
     *
     * @param properties the properties
     */
    public ConsoleResourceBundle(Properties properties) {
        super();
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getKeys() {
        return (Enumeration<String>) properties.propertyNames();
    }

    @Override
    protected Object handleGetObject(String key) {
        return properties.get(key);
    }

}
