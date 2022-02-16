/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2019, 2021 Michael N. Lipp
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

import { reactive, createApp, computed, defineComponent }
    from "../../page-resource/vue/vue.esm-browser.js";
import JGConsole from "../../console-base-resource/jgconsole.js"
import JgwcPlugin, { JGWC } 
    from "../../page-resource/jgwc-vue-components/jgwc-components.js";

const l10nBundles = new Map();
let entries = null;
// <#list supportedLanguages() as l>
entries = new Map();
l10nBundles.set('${l.locale.toLanguageTag()}', entries);
// <#list l.l10nBundle.keys as key>
entries.set('${key}', '${l.l10nBundle.getString(key)}')
// </#list>
// </#list>    

window.orgJGrapesOsgiConletStyleTest = {};

window.orgJGrapesOsgiConletStyleTest.initView = function(content) {
    let app = createApp({
        setup() {
            const conletId = $(content).closest("[data-conlet-id]").data("conlet-id");
            const controller = reactive(new JGConsole.TableController([
                ["year", 'Year'],
                ["month", 'Month'],
                ["title", 'Title'],
                ], {
                sortKey: "year"
            }));
            const detailsByKey = reactive({});
            const issues = [
                { year: 2019, month: 6, title: "Middle of year" },
                { year: 2019, month: 12, title: "End of year" },
                { year: 2020, month: 1, title: "A new year begins" },
            ];
            const filteredData = computed(() => {
                let infos = Object.values(issues);
                return controller.filter(infos);
            });
            const localize = (key) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang(), key);
            }
            
            const idScope = JGWC.createIdScope();
            
            const toggleMessages = reactive([]);
            
            const logToggleMessage = (msg) => {
                if (toggleMessages.length >= 10) {
                    toggleMessages.shift();
                }
                toggleMessages.push(msg);
            }
            
            return { conletId, controller, detailsByKey, issues, filteredData,
                localize, scopedId: (id) => { return idScope.scopedId(id); },
                logToggleMessage, toggleMessages };
        }
    });
    app.use(JgwcPlugin);
    app.config.globalProperties.window = window;
    
    app.component('toggle-event-list', defineComponent({
        props: { messages: { type: Array, required: true } },
        setup(props) {
            
            const messages = computed(() => props.messages);
            
            return { messages };
        },
        template: `
            <div>
              <ul>
                <li v-for="msg in messages">{{ msg }}</li>
              </ul>
            </div>`
    }));
    
    app.mount($(content)[0]);
}
