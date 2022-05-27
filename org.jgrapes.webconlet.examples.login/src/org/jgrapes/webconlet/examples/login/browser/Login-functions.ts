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

import { reactive, ref, createApp, computed, onMounted } from "vue";
import JGConsole from "jgconsole"
import JgwcPlugin, { JGWC } from "jgwc";
import l10nBundles from "l10nBundles";

// For global access
declare global {
    interface Window {
        orgJGrapesExampleLogin: any;
    }
}

window.orgJGrapesExampleLogin = {}

interface AccountData {
    website: string;
    username: string;
    password: string;
}

let accountData: AccountData = reactive({
    website: "",
    username: "",
    password: ""
});

window.orgJGrapesExampleLogin.initDialog = function(dialogDom: HTMLElement) {
    let app = createApp({
        setup() {
            const submitData = (event: Event) => {
                let conletId = dialogDom
                    .closest("[data-conlet-id]")!.getAttribute("data-conlet-id")!;
                JGConsole.notifyConletModel(conletId, "accountData", 
                    accountData.website, accountData.username, accountData.password);
            };
            
            const localize = (key: string) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang()!, key);
            };
                        
            return { localize, submitData, accountData };
        }
    });
    app.use(JgwcPlugin);
    app.mount(dialogDom);
}

window.orgJGrapesExampleLogin.apply = function(dialogDom: HTMLElement,
    apply: boolean, close: boolean) {
    return;
}
