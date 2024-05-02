/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.DisplayNotification;
import org.jgrapes.webconsole.base.events.OpenModalDialog;
import org.jgrapes.webconsole.base.events.RenderConlet;

/**
 * This interface provides web console component related constants. 
 * Web console components need not implement this interface, not even 
 * as a marker interface. They only have to expose a specific response 
 * behavior to certain events. An overview of the event sequences is 
 * provided by the package description. Make sure to read this 
 * description first.
 */
public interface Conlet {

    /**
     * The render modes.
     */
    @SuppressWarnings("PMD.FieldNamingConventions")
    enum RenderMode {
        /**
         * Provide content to display in a card on the overview tab
         * (by firing a {@link RenderConlet} event). 
         */
        Preview,
        /**
         * Provide content to display in a tab of its own
         * (by firing a {@link RenderConlet} event).
         */
        View,
        /**
         * Provide a dialog for modifying a conlet instance's
         * properties, typically by opening a modal dialog
         * (see {@link OpenModalDialog}).
         */
        Edit,
        /**
         * Provide help information, typically by opening a modal dialog
         * (see {@link OpenModalDialog}) or using a notification
         * (see {@link DisplayNotification}).
         */
        Help,
        /**
         * Provide content that is to be embedded in an area of the
         * console page (see 
         * {@link AddConletType#addPageContent(String, java.util.Map)}) 
         * or in some other conlet's content (see {@link RenderConlet}).
         * 
         * Conlets that want to include another conlet's 
         * "content" must generate an HTML element as container 
         * (typically a `div` or `span`) with an attribute 
         * "data-conlet-type" and classes "conlet conlet-content". 
         * Additional attributes "data-conlet-..." 
         * are considered properties of the conlet instance to be added 
         * and are passed to the server with the {@link AddConletRequest}. 
         * 
         * Property "id" may be used if already known, but is usually 
         * assigned later by the component implementation when the 
         * component is rendered. Property "state" is reserved. 
         * Its value is provided by the console. The handler for 
         * {@link AddConletRequest} should pass the value of "state"
         * back as attribute `data-conlet-state` of the root 
         * (or first) element of the content. This allows the console 
         * to match the content provided with the conlet content container
         * that caused the request.
         */
        Content,
        /** 
         * Modifier, indicates that a {@link #Preview} may not be removed.
         */
        StickyPreview,
        /** 
         * Modifier, forces rendered view to be put in foreground. 
         */
        Foreground;

        /**
         * The basic modes (the modes without modifiers).
         */
        @SuppressWarnings("PMD.VariableNamingConventions")
        public static final Set<RenderMode> basicModes
            = Collections.unmodifiableSet(asSet(Preview, View, Edit, Help));

        /**
         * Utility method that creates a {@link Set} of render modes
         * from enumerated values.
         *
         * @param modes the modes
         * @return the sets the
         */
        public static Set<RenderMode> asSet(RenderMode... modes) {
            return new HashSet<>(Arrays.asList(modes));
        }

        /**
         * Retrieves the modifiers.
         */
        public static Set<RenderMode> modifiers(Set<RenderMode> modes) {
            Set<RenderMode> result = new HashSet<>(modes);
            result.removeAll(basicModes);
            return result;
        }

        /**
         * Adds the modifiers from the given set to this basic mode.
         *
         * @param modifiers the set with modifiers
         * @return the augmented basic mode
         */
        public Set<RenderMode> addModifiers(Set<RenderMode> modifiers) {
            Set<RenderMode> result = modifiers(modifiers);
            result.add(this);
            return result;
        }
    }

}
