/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

/**
 * This package provides a very basic, sample web console configuration
 * which can be used for testing and demonstration purposes. It provides
 * the web console with HTTP (port 8888) and HTTPS (port 8443) access.
 * 
 * When started with page providers and web conlets in the classpath,
 * these will be automatically picked up and added to the console 
 * configuration.
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.jgrapes.webconsole.examples.consoleapp;
