/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
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

import { reactive, ref, createApp, computed, watch }
    from "../../page-resource/vue/vue.esm-browser.js";
import JGConsole from "../../console-base-resource/jgconsole.js"
import JgwcPlugin, { JGWC } 
    from "../../page-resource/jgwc-vue-components/jgwc-components.js";
import { provideApi, getApi } 
    from "../../page-resource/aash-vue-components/lib/aash-vue-components.js";
import l10nBundles from "./LogViewer-l10nBundles.ftl.js";

window.orgJGrapesConletLogViewer = {};

window.orgJGrapesConletLogViewer.initView = function(content) {
    let levelsToNum = {
            "SEVERE": 6,
            "WARNING": 5,
            "INFO": 4,
            "CONFIG": 3,
            "FINE": 2,
            "FINER": 1,
            "FINEST": 0
    } 
    let app = createApp({
        setup() {
            const conletId = content.closest("[data-conlet-id]")
                .dataset["conletId"];
            const ctrlL18n = (key) => 
                JGConsole.localize(l10nBundles, JGWC.lang(), key);
            const controller = reactive(new JGConsole.TableController([
                ["time", ctrlL18n],
                ["logLevel", ctrlL18n],
                ["message", ctrlL18n],
                ["source", ctrlL18n],
                ["exception", ctrlL18n],
                ], {
                sortKey: "sequence",
                sortOrder: "down"
            }));
            const messageThreshold = ref("INFO");
            const entries = reactive([]);
            const autoUpdate = ref(true);
            const expandedByKey = reactive({});
            const filteredData = computed(() => {
                let filtered = [];
                let threshold = levelsToNum[messageThreshold.value];
                for (let entry of entries) {
                    if (levelsToNum[entry.logLevel] >= threshold) {
                        filtered.push(entry);
                    }
                }
                return controller.filter(filtered);
            });
            const resync = () => {
                JGConsole.notifyConletModel(conletId, "resync");
            };
            const formatter = computed(() => {
                return new Intl.DateTimeFormat(JGWC.lang(), 
                        { year: "numeric", month: "numeric", day: "numeric",
                          hour: "numeric", minute: "numeric", 
                          hour12: false,
                          second: "numeric", fractionalSecondDigits: 3 });
            })
            const formatTimestamp = (timestamp) => {
                return formatter.value.format(timestamp);
            };
            const toggleExpanded = (key) => {
                if (key in expandedByKey) {
                    expandedByKey.delete(key);
                    return;
                }
                expandedByKey[key] = true;
            };
            const isExpanded = (key) => {
                return (key in expandedByKey);                
            };
            watch(autoUpdate, (newValue, oldValue) => {
                if (newValue) {
                    resync();
                }
            });
            
            provideApi (content, {
                clearEntries: () => { entries.length = 0; },
                addEntries: function() {
                    entries.push(...arguments);
                    if (entries.length > 100) {
                        entries.splice(0, entries.length - 100);
                    }
                },
                isAutoUpdate: () => { return autoUpdate.value; }
            });
            
            const idScope = JGWC.createIdScope();

            return { autoUpdate, resync, messageThreshold, controller,
                filteredData, formatTimestamp, toggleExpanded, isExpanded,
                scopedId: (id) => { return idScope.scopedId(id); } };
        }
    });
    app.use(JgwcPlugin);
    app.mount(content);
}

window.orgJGrapesConletLogViewer.onUnload = function(content) {
    JGWC.unmountVueApps(content);
    for (let child of content.children) {
        window.orgJGrapesConletBundles.onUnload(child);
    }
}

JGConsole.registerConletFunction(
    "org.jgrapes.webconlet.logviewer.LogViewerConlet",
    "entries", function(conletId, entries) {
        // View only
        let conlet = JGConsole.findConletView(conletId);
        if (conlet == null) {
            return;
        }
        let view = conlet.element().querySelector(":scope .jgrapes-logviewer-view");
        if (view === 0) {
            return;
        }
        let api = getApi(view);
        if (api == null) {
            return;
        }
        api.clearEntries();
        api.addEntries(...entries);
    });

JGConsole.registerConletFunction(
    "org.jgrapes.webconlet.logviewer.LogViewerConlet",
    "addEntry", function(conletId, entry) {
        // View only
        let conlet = JGConsole.findConletView(conletId);
        if (conlet == null) {
            return;
        }
        let view = conlet.element().querySelector(":scope .jgrapes-logviewer-view");
        if (view === 0) {
            return;
        }
        let api = getApi(view);
        if (api == null) {
            return;
        }
        if (!api.isAutoUpdate()) {
            return;
        }
        api.addEntries(entry);
     });

