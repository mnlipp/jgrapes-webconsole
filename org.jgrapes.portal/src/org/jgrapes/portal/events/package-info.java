/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

/**
 * The events used by the portal related components.
 * 
 * Events that end with "...Cmd" are transformed to JSON messages
 * that are sent to the portal session. They may only be fired
 * on the {@link org.jgrapes.portal.PortalSession#responsePipeline()}
 * (usually with 
 * {@link org.jgrapes.portal.PortalSession#respond(org.jgrapes.core.Event)}).
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.portal.events;