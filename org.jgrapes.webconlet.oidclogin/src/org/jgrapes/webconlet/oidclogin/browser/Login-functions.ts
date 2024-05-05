/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022,2024 Michael N. Lipp
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

import { reactive, ref, createApp } from "vue";
import JGConsole from "jgconsole"
import JgwcPlugin, { JGWC } from "jgwc";
import { provideApi, getApi, AashDropdownMenu } from "aash-plugin";
import l10nBundles from "l10nBundles";

// For global access
declare global {
    interface Window {
        orgJGrapesOidcLogin: any;
    }
}

window.orgJGrapesOidcLogin = window.orgJGrapesOidcLogin || {};

interface AccountData {
    username: string;
    password: string;
}

let openLocalDialog = function(dialogDom: HTMLElement, isUpdate: boolean) {
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

            const info = ref<string|null>(null);
            const warning = ref<string|null>(null);
            
            JGConsole.registerConletFunction(
                "org.jgrapes.webconlet.oidclogin.LoginConlet",
                "setMessages", function(conletId, infoMsg, warnMsg) {
                info.value = infoMsg;
                warning.value = warnMsg;
                });

            const formDom = ref(null);

            provideApi(formDom, accountData);
                        
            return { formDom, formId, localize, accountData, info, warning };
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
              <p v-if="info" class="local-login-form__info">
                {{ info }}
              </p>
              <p v-if="warning" class="local-login-form__warning">
                {{ warning }}
              </p>
            </fieldset>
          </form>`
    });
    app.use(JgwcPlugin);
    app.mount(dialogDom);
}

let openOidcDialog = function(dialogDom: HTMLElement, isUpdate: boolean) {
    if (isUpdate) {
        return;
    }
    let app = createApp({
        setup() {
            const conlet = <HTMLElement>dialogDom
                .closest("*[data-conlet-id]")!
            const conletId = conlet.dataset.conletId!;
            const formId = conlet.id + "-form";
            const hasUser = dialogDom.dataset["hasUser"] === "yes";
            const providers = window.orgJGrapesOidcLogin.providers;

            const accountData: AccountData = reactive({
                username: "",
                password: ""
            });

            const localize = (key: string) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang()!, key);
            };

            const info = ref<string|null>(null);
            const warning = ref<string|null>(null);
            
            JGConsole.registerConletFunction(
                "org.jgrapes.webconlet.oidclogin.LoginConlet",
                "setMessages", function(conletId, infoMsg, warnMsg) {
                info.value = infoMsg;
                warning.value = warnMsg;
                });

            const formDom = ref(null);

            const apply = () => {
                window.orgJGrapesOidcLogin.apply(dialogDom, true, true);
            }

            const chooseProvider = (name: string) => {
                let provider = providers
                  .filter((p: Map<string,string>) => p.get("name") === name)[0];
                let popupFactor = provider.get("popupFactor") || 0.8;
                let pH = provider.get("popupHeight") 
                    || Math.round(window.outerHeight * popupFactor);
                let pW = provider.get("popupWidth")
                    || Math.round(window.outerWidth * popupFactor);
                let popupX = Math.round(window.screenX 
                    + (window.outerWidth - pW) / 2);
                let popupY = Math.round(window.screenY 
                    + (window.outerHeight -pH) / 2);
                window.orgJGrapesOidcLogin 
                    = window.open("about:blank", "_new", "popup"
                    + ",width=" + pW + ",height=" + pH
                    + ",screenX=" + popupX + ",screenY=" + popupY);
                JGConsole.notifyConletModel(conletId, "useProvider", name);
            }

            provideApi(formDom, accountData);
                        
            return { formDom, formId, localize, accountData, info, warning,
                apply, hasUser, providers, chooseProvider };
        },
        template: `
            <p v-if="hasUser">
              <aash-accordion :header-type="'p'">
                <aash-accordion-section :title="localize('Local Login')">
                  <form :id="formId" ref="formDom" onsubmit="return false;">
                    <fieldset>
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
                      <p v-if="info" class="oidc-login-form__info">
                        {{ info }}
                      </p>
                      <p v-if="warning" class="oidc-login-form__warning">
                        {{ warning }}
                      </p>
                    </fieldset>
                    <p>
                      <button :form="formId" type="submit"
                        v-on:click="apply()">{{ localize('Log in') }}</button>
                    </p>
                  </form>
                </aash-accordion-section>
              </aash-accordion>
            </p>
            <p v-for="p of providers">
              <button v-on:click="chooseProvider(p.get('name'))"> 
                {{ p.get('displayName') }} </button>
            </p>`
    });
    app.use(JgwcPlugin);
    app.mount(dialogDom);
}

window.orgJGrapesOidcLogin.openDialog 
    = function(dialogDom: HTMLElement, isUpdate: boolean) {
      if (window.orgJGrapesOidcLogin.providers.size === 0) {
          openLocalDialog(dialogDom, isUpdate);
          return;
      }
      openOidcDialog(dialogDom, isUpdate);
}

window.orgJGrapesOidcLogin.apply = function(dialogDom: HTMLElement,
    apply: boolean, close: boolean) {
    if (!apply) {
        return;
    }
    const conletId = (<HTMLElement>dialogDom.closest("[data-conlet-id]")!)
        .dataset["conletId"]!;
    const accountData = getApi<AccountData>
        (dialogDom.querySelector(":scope form")!)!;
    JGConsole.notifyConletModel(conletId, "loginData", 
        accountData.username, accountData.password);
    return;
}

window.orgJGrapesOidcLogin.initStatus 
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
                "org.jgrapes.webconlet.oidclogin.LoginConlet",
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

JGConsole.registerConletFunction(
    "org.jgrapes.webconlet.oidclogin.LoginConlet",
    "openLoginWindow", function(conletId: string, url: string) {
        window.orgJGrapesOidcLogin.location = url;
//        window.open(url, "_new", "popup");
    });
