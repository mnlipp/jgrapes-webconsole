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

package org.jgrapes.portal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This interface provides portlet related constants. Portlets
 * need not implement this interface, not even as a marker interface.
 * They only have to expose a specific response behavior to certain
 * events. An overview of the event sequences is provided by the
 * package description. Make sure to read this description first.
 */
public interface Portlet {
	
	enum RenderMode { Preview, DeleteablePreview, View, Edit, Help;
		public static Set<RenderMode> asSet(RenderMode... modes) {
			return new HashSet<>(Arrays.asList(modes));
		}
	}

}
