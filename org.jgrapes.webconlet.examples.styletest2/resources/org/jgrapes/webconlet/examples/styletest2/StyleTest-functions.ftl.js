/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
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

import JGConsole from "../../console-base-resource/jgconsole.js"
import Alpine from "../../page-resource/alpinejs/module.esm.js"
import { aashId } from "../../page-resource/aash-alpinejs/aash-alpinejs-components.js"

const l10nBundles = new Map();
let entries = null;
// <#list supportedLanguages() as l>
entries = new Map();
l10nBundles.set('${l.locale.toLanguageTag()}', entries);
// <#list l.l10nBundle.keys as key>
entries.set('${key}', '${l.l10nBundle.getString(key)}')
// </#list>
// </#list>    

let htmlRoot = document.querySelector("html");

window.orgJGrapesConletStyleTest2 = {};

orgJGrapesConletStyleTest2.mark = () => {
    console.log("Mark");
}

let cleanupLocaleCallback = null;

Alpine.data("orgJGrapesConletStyleTest2", () => ({
    locale: htmlRoot.getAttribute('lang') || 'en',
    
    localize(key) {
        return JGConsole.localize(l10nBundles, this.locale, key);
    },
    
    toggleMessages: [],
    
    logToggleMessage(msg) {
        if (this.toggleMessages.length >= 10) {
            this.toggleMessages.shift();
        }
        this.toggleMessages.push(msg);
    },
    
    init() {
        cleanupLocaleCallback = JGConsole.instance.whenLocaleChanged(
            (locale) => { this.locale = locale; });        
    }
}))

window.orgJGrapesConletStyleTest2.initView = function(content) {
}

window.orgJGrapesConletStyleTest2.cleanup = function(content) {
    if (cleanupLocaleCallback) {
        cleanupLocaleCallback();
    }
}