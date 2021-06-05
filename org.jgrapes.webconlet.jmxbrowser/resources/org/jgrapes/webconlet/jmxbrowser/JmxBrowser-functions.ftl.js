/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018,2021 Michael N. Lipp
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

import JGConsole from "../../console-base-resource/jgconsole.esm.js";
import { createApp } from "../../page-resource/vue/vue.esm-browser.js";
import AashPlugin, { getApi }
    from "../../page-resource/aash-vue-components/lib/aash-vue-components.js";

window.orgJGrapesWebconletJmxBrowser = {};

window.orgJGrapesWebconletJmxBrowser.initPreview = function(preview) {
    const app = createApp({});
    app.use(AashPlugin);
    app.config.globalProperties.window = window;
    app.mount(preview);
};

window.orgJGrapesWebconletJmxBrowser.initView = function(view) {
    const app = createApp({
        setup(_props) {
            const conletId = view.parentNode.dataset["conletId"];

            const selectMBean = (path, even) => {
                JGConsole.notifyConletModel(conletId, "sendMBean", path);
            }

            return { selectMBean }            
        }
    });
    app.use(AashPlugin);
    app.config.globalProperties.window = window;
    app.mount(view);
};

window.orgJGrapesWebconletJmxBrowser.onUnload = function(content) {
    if(content.__vue_app__) {
        content.__vue_app__.unmount();
    }
};

JGConsole.registerConletFunction(
        "org.jgrapes.webconlet.jmxbrowser.JmxBrowserConlet",
        "mbeansTree", function(conletId, ...params) {
            // Preview
    if (params[1] === "preview" || params[1] === "*") {
        let preview = JGConsole.findConletPreview(conletId);
        let tree = preview.querySelector(":scope [role='tree']");
        let treeApi = getApi(tree);
        treeApi.setRoots(params[0]);
    }
                
    if (params[1] === "view" || params[1] === "*") {
        let view = JGConsole.findConletView(conletId);
        let tree = view.querySelector(":scope [role='tree']");
        let treeApi = getApi(tree);
        treeApi.setRoots(params[0]);
    }
                
});

