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

import JGConsole from "@JGConsole"
import { createApp } from "@Vue";
import AashPlugin, { getApi, AashTreeView } from "@Aash";

import "./JmxBrowser-style.scss"

// For global access
declare global {
  interface Window {
    orgJGrapesWebconletJmxBrowser: any;
  }
}

window.orgJGrapesWebconletJmxBrowser = {};

window.orgJGrapesWebconletJmxBrowser.initPreview 
        = (preview: HTMLElement, isUpdate: boolean) => {
    if (isUpdate) {
        return;
    }
    const app = createApp({});
    app.use(AashPlugin);
    app.config.globalProperties.window = window;
    app.mount(preview);
};

window.orgJGrapesWebconletJmxBrowser.initView 
        = (view: HTMLElement, isUpdate: boolean) => {
    if (isUpdate) {
        return;
    }
    const app = createApp({
        setup(_props: any) {
            const conletId: string 
                = (<HTMLElement>view.parentNode!).dataset["conletId"]!;

            const selectMBean = (path: string[], _event: Event) => {
                JGConsole.notifyConletModel(conletId, "sendMBean", path);
            }

            return { selectMBean }            
        }
    });
    app.use(AashPlugin);
    app.config.globalProperties.window = window;
    app.mount(view);
};

window.orgJGrapesWebconletJmxBrowser.onUnload 
        = function(content: any, isUpdate: boolean) {
    if (isUpdate) {
        return;
    }
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
        let tree = <HTMLElement>preview?.querySelector(":scope [role='tree']");
        let treeApi = <AashTreeView.Api>getApi(tree);
        treeApi.setRoots(params[0]);
    }
                
    if (params[1] === "view" || params[1] === "*") {
        let view = JGConsole.findConletView(conletId);
        let tree = <HTMLElement>view?.querySelector(":scope [role='tree']");
        let treeApi = <AashTreeView.Api>getApi(tree);
        treeApi.setRoots(params[0]);
    }
                
});

