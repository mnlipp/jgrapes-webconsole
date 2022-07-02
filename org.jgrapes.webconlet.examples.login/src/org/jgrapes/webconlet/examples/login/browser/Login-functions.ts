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
import { provideApi, getApi, AashDropdownMenu } from "aash-plugin";
import l10nBundles from "l10nBundles";

// For global access
declare global {
    interface Window {
        orgJGrapesExampleLogin: any;
    }
}

window.orgJGrapesExampleLogin = {}

interface AccountData {
    username: string;
    password: string;
}

window.orgJGrapesExampleLogin.openDialog 
    = function(dialogDom: HTMLElement, isUpdate: boolean) {
    if (isUpdate) {
        return;
    }
    let app = createApp({
        setup() {
            const formId = (<HTMLElement>dialogDom
                .closest("*[data-conlet-id]")!).id + "-form";

            const accountData: AccountData = reactive({
                username: "",
                password: ""
            });

            const localize = (key: string) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang()!, key);
            };

            const formDom = ref(null);

            provideApi(formDom, accountData);
                        
            return { formDom, formId, localize, accountData };
        },
        template: `
          <form :id="formId" ref="formDom" onsubmit="return false;">
            <fieldset>
              <legend>{{ localize("Login Data") }}</legend>
              <p>
                <label class="form__label--full-width">
                  <span>
                    {{ localize("User Name") }}
                    <strong>
                      <abbr v-bind:title='localize("required")'>*</abbr>
                    </strong>
                  </span>
                  <input type="text" name="username" v-model="accountData.username"
                    autocomplete="section-test username">
                </label>
              </p>
              <p>
                <label class="form__label--full-width">
                  <span>
                    {{ localize("Password") }}
                    <strong>
                      <abbr v-bind:title='localize("required")'>*</abbr>
                    </strong>
                  </span>
                  <input type="password" name="password" v-model="accountData.password"
                    autocomplete="section-test current-password">
                </label>
              </p>
            </fieldset>
          </form>`
    });
    app.use(JgwcPlugin);
    app.mount(dialogDom);
}

window.orgJGrapesExampleLogin.apply = function(dialogDom: HTMLElement,
    apply: boolean, close: boolean) {
    const conletId = (<HTMLElement>dialogDom.closest("[data-conlet-id]")!)
        .dataset["conletId"]!;
    const accountData = getApi<AccountData>
        (dialogDom.querySelector(":scope form")!)!;
    JGConsole.notifyConletModel(conletId, "loginData", 
        accountData.username, accountData.password);
    return;
}

window.orgJGrapesExampleLogin.initStatus 
    = function(container: HTMLElement, isUpdate: boolean) {
    if (isUpdate) {
        return;
    }
    const conletId = (<HTMLElement>container.closest("*[data-conlet-id]")!)
        .dataset["conletId"]!;
    let app = createApp({
        setup() {
            const name = ref<string | null>(null);
            
            JGConsole.registerConletFunction(
                "org.jgrapes.webconlet.examples.login.LoginConlet",
                "updateUser", function(conletId: string, newName: string) {
                    name.value = newName;
                });
            
            const localize = (key: string) =>
                JGConsole.localize(l10nBundles, JGWC.lang()!, key)

            const action = (arg: AashDropdownMenu.MenuItem) => {
                JGConsole.notifyConletModel(conletId, "logout");
            }
            
            return { localize, name, action };
        }
    });
    app.use(JgwcPlugin);
    app.mount(container);
}
