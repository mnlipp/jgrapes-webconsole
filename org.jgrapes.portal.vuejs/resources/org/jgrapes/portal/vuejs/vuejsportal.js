/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2019  Michael N. Lipp
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

'use strict';

import JGPortal from "../portal-base-resource/jgportal.js"

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 * 
 * @module vuejsportal
 */
export const VueJsPortal = {};
export default VueJsPortal;

var log = JGPortal.log;

VueJsPortal.Renderer = class extends JGPortal.Renderer {

    constructor(l10n) {
        super();
    }

    init() {
    }

    connectionLost() {
    }

    connectionRestored() {
    }

    connectionSuspended(resume) {
    }

    portalConfigured() {
    }

    addPortletType(portletType, displayName, renderModes) {
    }

    lastPortalLayout(previewLayout, tabsLayout, xtraInfo) {
    }

    updatePortletPreview(isNew, container, modes, content, foreground) {
    }

    updatePortletView(isNew, container, modes, content, foreground) {
    }

    portletRemoved(containers) {
    }

    notification(content, options) {
    }

    /**
     * Update the title of the portlet with the given id.
     *
     * @param {string} portletId the portlet id
     * @param {string} title the new title
     */
    updatePortletTitle(portletId, title) {
    }

    /**
     * Update the modes of the portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     * @param {string[]} modes the modes
     */
    updatePortletModes(portletId, modes) {
    }

    showEditDialog(container, modes, content) {
    }
}
