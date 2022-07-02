/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2022  Michael N. Lipp
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

import Log from "./Log";
import ResourceManager from "./ResourceManager";
import RenderMode from "./RenderMode";
import Renderer from "./Renderer";
import { Conlet, DefaultConlet } from "./Renderer";
import ConsoleWebSocket from "./ConsoleWebSocket";
import NotificationOptions from "./NotificationOptions";
import ModalDialogOptions from "./ModalDialogOptions";
import ConsoleNotification from "./Notification";
import { parseHtml } from "./Util";

interface PageComponentSpecification {
    area: String;
    properties: any;
}

/**
 * Provides the console related functions.
 */
class Console {
    private _isConfigured = false;
    private _sessionRefreshInterval = 0;
    private _sessionInactivityTimeout = 0;
    private _renderer: Renderer | null = null;
    private _webSocket = new ConsoleWebSocket(this);
    private _conletFunctionRegistry = new Map<string, 
        Map<string, (conletId: string, ...args: any) => void>>();
    private _previewTemplate = parseHtml(
        '<section class="conlet conlet-preview"></section>')[0];
    private _viewTemplate = parseHtml(
        '<article class="conlet conlet-view conlet-body"></article>')[0];
    private _modalDialogTemplate = parseHtml(
        '<div class="conlet conlet-modal-dialog"></div>')[0];
    private _resourceManager: ResourceManager;
    private _dialogIdCounter = 0;
    private _pageComponentCounter = 0;

    constructor() {
        document.querySelector("body")?.append(parseHtml(
            '<div id="console-modal-slot"></div>')[0]);
        let _this = this;
        this._resourceManager = new ResourceManager(this);
        this._webSocket.addMessageHandler('addPageResources',
            (cssUris, cssSource, scriptResources) => {
                _this._resourceManager.addPageResources(
                    cssUris, cssSource, scriptResources);
            });
        this._webSocket.addMessageHandler('addConletType',
            function(conletType, displayNames: any, cssUris, 
                scriptResources, renderModes: RenderMode[],
                pageComps: PageComponentSpecification[]) {
                _this._resourceManager.addPageResources
                    (cssUris, null, scriptResources);
                let asMap = new Map<string,string>();
                for (let k of Object.keys(displayNames)) {
                    asMap.set(k, displayNames[k]);
                }
                _this._renderer!.addConletType
                    (conletType, asMap, renderModes, pageComps);
            });
        this._webSocket.addMessageHandler('removeConletType',
            function(conletType) {
                _this._renderer!.removeConletType(conletType);
            });
        this._webSocket.addMessageHandler('lastConsoleLayout',
            (previewLayout, tabsLayout, xtraInfo) => {
                // Should we wait with further actions?
                _this._resourceManager.lockWhileLoading();
                _this._renderer!.lastConsoleLayout
                    (previewLayout, tabsLayout, xtraInfo);
                _this._resolveComponents();
            });
        this._webSocket.addMessageHandler('notifyConletView',
            (conletClass, conletId, method, params) => {
                let classRegistry = _this._conletFunctionRegistry.get(conletClass);
                if (classRegistry) {
                    let f = classRegistry.get(method);
                    if (f) {
                        f(conletId, ...params);
                    }
                }
            });
        this._webSocket.addMessageHandler('consoleConfigured',
            () => {
                _this._isConfigured = true;
                _this._renderer!.consoleConfigured();
            });
        this._webSocket.addMessageHandler('updateConlet',
            (conletType, conletId, renderAs, supported, content) => {
                if (renderAs.includes(RenderMode.Preview)) {
                    _this._updatePreview(conletType, conletId, supported, content, 
                    renderAs.includes(RenderMode.StickyPreview),
                    renderAs.includes(RenderMode.Foreground));
                } else if (renderAs.includes(RenderMode.View)) {
                    _this._updateView(conletType, conletId, supported, content,
                    renderAs.includes(RenderMode.Foreground));
                } else if (renderAs.includes(RenderMode.Content)) {
                    _this._updateContent(conletType, conletId, supported, content);
                }
            });
        this._webSocket.addMessageHandler('deleteConlet',
            (conletId, renderModes) => {
                if (renderModes.length === 0 
                    || renderModes.includes(RenderMode.Preview)) {
                    _this.removePreview(conletId);
                }
                if (renderModes.includes(RenderMode.View)) {
                    _this.removeView(conletId);
                }
            });
        this._webSocket.addMessageHandler('displayNotification',
            (content, options) => {
                _this._renderer!.notification(content, options);
            });
        this._webSocket.addMessageHandler('openModalDialog',
            (conletType, conletId, content, options: ModalDialogOptions) => {
                if (_this._resourceManager.lockWhileLoading()) {
                    this._webSocket.postponeMessage();
                    return;
                }
                let container 
                    = <HTMLElement>_this._modalDialogTemplate.cloneNode(true);
                container.dataset["conletType"] = conletType;
                container.dataset["conletId"] = conletId;
                container.id = "jgwc-modal-dialog-" + ++_this._dialogIdCounter;
                _this._renderer!.openModalDialog(container, options, content);
                if (!container.parentNode) {
                    // Must be attached to DOM tree
                    let slot = document.querySelector("#console-modal-slot");
                    if (slot) {
                        while (slot!.firstChild) {
                            slot!.removeChild(slot!.firstChild);
                        }
                        slot!.append(container);
                    }
                }
                _this._execOnLoad(container, false);
            });
        this._webSocket.addMessageHandler('closeModalDialog',
            (conletType, conletId) => {
                let container = this._renderer!.findModalDialog(conletId);
                if (container) {
                    _this._renderer!.closeModalDialog(container);
                    _this._execOnLoad(container, false);
                }
        });
        this._webSocket.addMessageHandler('retrieveLocalData',
            (path) => {
                let result = [];
                try {
                    for (let i = 0; i < localStorage.length; i++) {
                        let key = localStorage.key(i);
                        if (!path.endsWith("/")) {
                            if (key !== path) {
                                continue;
                            }
                        } else {
                            if (!key!.startsWith(path)) {
                                continue;
                            }
                        }
                        let value = localStorage.getItem(key!);
                        result.push([key, value])
                    }
                } catch (e) {
                    Log.error(e);
                }
                _this._webSocket.send({
                    "jsonrpc": "2.0", "method": "retrievedLocalData",
                    "params": [result]
                });
            });
        this._webSocket.addMessageHandler('storeLocalData',
            (actions) => {
                try {
                    for (let i in actions) {
                        let action = actions[i];
                        if (action[0] === "u") {
                            localStorage.setItem(action[1], action[2]);
                        } else if (action[0] === "d") {
                            localStorage.removeItem(action[1]);
                        }
                    }
                } catch (e) {
                    Log.error(e);
                }
            });
        this._webSocket.addMessageHandler('reload',
            () => { window.location.reload(); });
    }

    /**
     * Starts the websocket connection to the server, invokes
     * {@link Renderer.init} and sends the `consoleReady` message
     * to the server.
     *
     * @param consoleSessionId the session id
     * @param options additional options
     */
    init(consoleSessionId: string, 
        options: { 
            refreshInterval?: number,
            inactivityTimeout?: number
        }) {
        Log.debug("Initializing console...");
        sessionStorage.setItem("org.jgrapes.webconsole.base.sessionId", consoleSessionId);
        this._resourceManager = new ResourceManager(this);
        if (options["refreshInterval"]) {
            this._sessionRefreshInterval = options.refreshInterval;            
        }
        if (options["inactivityTimeout"]) {
            this._sessionInactivityTimeout = options.inactivityTimeout;
        }

        // Everything set up, can connect web socket now.
        this._webSocket.connect();

        // More initialization.
        Log.debug("Initializing renderer...");
        this._renderer?.init(options);

        // With everything prepared, send console ready
        this.send("consoleReady");
        Log.debug("ConsoleReady sent.");
    }

    get configured() {
        return this._isConfigured;
    }

    get sessionRefreshInterval() {
        return this._sessionRefreshInterval;
    }

    get sessionInactivityTimeout() {
        return this._sessionInactivityTimeout;
    }

    set renderer(renderer: Renderer) {
        this._renderer = renderer;
    } 

    /**
     * Invokes {@link Renderer.connectionLost}.
     */
    connectionLost() {
        this._renderer!.connectionLost();
    }

    /**
     * Invokes {@link Renderer.connectionRestored}.
     */
    connectionRestored() {
        this._renderer!.connectionRestored();
    }

    /**
     * Invokes {@link Renderer.connectionSuspended}.
     */
    connectionSuspended(resume: () => void) {
        this._renderer!.connectionSuspended(resume);
    }

    /**
     * Increases the lock count on the receiver. As long as
     * the lock count is greater than 0, the invocation of
     * handlers is suspended.
     */
    lockMessageQueue() {
        this._webSocket.lockMessageReceiver();
    }

    /**
     * Decreases the lock count on the receiver. When the
     * count reaches 0, the invocation of handlers is resumed.
     */
    unlockMessageQueue() {
        this._webSocket.unlockMessageReceiver();
    }

    // Conlet management

    private _addConletProperties(properties: Map<string,string>,
            element: HTMLElement) {
        for (let key in element.dataset) {
            if (key.startsWith("conlet") && key.length >= 7
                && key.substring(6, 7).toUpperCase() == key.substring(6, 7)
                && key != "conletType" && key != "conletId" 
                && key.substring(6, 7) != "_") {
                let shortened = key.substring(6, 7).toLowerCase()
                    + key.substring(7);
                properties.set(shortened, element.dataset[key]!);
            }
        } 
    }

    public collectConletProperties(conlet: Conlet): Map<string,string> {
        let result = new Map<string,string>();
        this._addConletProperties(result, conlet.element());
        for (let i = 0; i < conlet.element().children.length; i++) {
            if (conlet.element().children[i] instanceof HTMLElement) {
                this._addConletProperties(result, 
                    <HTMLElement>conlet.element().children[i]);
            }
        }
        return result;
    }

    /**
     * Called when the console layout is received or when a conlet 
     * has been added or updated. Attempts to resolve all unresolved 
     * components, i.e. calls {@link Console#addConlet} for the conlets
     * that provide the missing component. 
     */
    private _resolveComponents() {
        let _this = this;
        let contents = _this._renderer!.findConletContents();
        contents.forEach((conlet: Conlet) => {
            if (!conlet.element().dataset["conletState"]) {
                // New (unresolved) component
                conlet.element().dataset["conletState"] = "resolving-" 
                    + this._pageComponentCounter++;
                let type = conlet.element().dataset["conletType"];
                if (type) {
                    _this.addConlet(type, [RenderMode.Content], 
                        _this.collectConletProperties(conlet));
                }
            }
        });
    }

    private _updateContent(conletType: string, conletId: string, 
            modes: RenderMode[], contentString: string) {
        let _this = this;
        let conlets = this._renderer!.findConletContents();
        let content = parseHtml(contentString);
        let conlet: Conlet | null = null;
        let isNew = true;
        for (let i = 0; i < conlets.length; i++) {
            // Check if update of existing
            if (conlets[i].id() === conletId) {
                conlet = conlets[i];
                isNew = false;
                break;
            }
            // Check if content for new (resolving)
            if (conlets[i].element().dataset["conletType"] == conletType
                && conlets[i].element().dataset["conletState"]
                && (content[0].dataset["conletState"]
                    // Targeted (repeats state) or first match 
                    ? content[0].dataset["conletState"] 
                        === conlets[i].element().dataset["conletState"]
                    : conlets[i].element().dataset["conletState"]!
                        .startsWith("resolving"))) {
                conlet = conlets[i];
                break;
            }
        }
        if (!conlet) {
            this.send("conletsDeleted", [conletId, [RenderMode.Content]]);
            return;
        }
        if (isNew) {
            conlet.element().dataset["conletId"] = conletId;
            conlet.element().dataset["conletState"] = "resolved";
            delete content[0].dataset["conletState"];
        } else {
            this._execOnUnload(conlet.element(), true);
        }
        this._renderer!.updateConletContent(conlet, content);
        this._execOnLoad(conlet.element(), !isNew);
        this._resolveComponents();
    }

    private _updatePreview(conletType: string, conletId: string, 
        modes: RenderMode[], content: string, sticky: boolean, foreground: boolean) {
        let conlet = this._renderer!.findConletPreview(conletId);
        let isNew = !conlet;
        if (isNew) {
            conlet = new DefaultConlet(<HTMLElement>this._previewTemplate
                .cloneNode(true));
            conlet.element().dataset["conletType"] = conletType;
            conlet.element().dataset["conletId"] = conletId;
        } else {
            this._execOnUnload(conlet!.element(), true);
        }
        if (sticky) {
            conlet!.element().classList.remove('conlet-deleteable');
        } else {
            conlet!.element().classList.add('conlet-deleteable');
        }
        this._renderer!.updateConletPreview(isNew, conlet!, modes,
            parseHtml(content), foreground);
        this._execOnLoad(conlet!.element(), !isNew);
        this._resolveComponents();
    }

    private _updateView(conletType: string, conletId: string, modes: RenderMode[], 
        content: string, foreground: boolean) {
        let conlet = this._renderer!.findConletView(conletId);
        let isNew = !conlet;
        if (isNew) {
            conlet = new DefaultConlet(<HTMLElement>this._viewTemplate
                .cloneNode(true));
            conlet.element().dataset["conletType"] = conletType;
            conlet.element().dataset["conletId"] = conletId;
        } else {
            this._execOnUnload(conlet!.element(), true);
        }
        this._renderer!.updateConletView(isNew, conlet!, modes,
            parseHtml(content), foreground);
        this._execOnLoad(conlet!.element(), !isNew);
        this._resolveComponents();
    }

    private _execOnLoad(container: HTMLElement, isUpdate: boolean) {
        container.querySelectorAll("[data-jgwc-on-load]").forEach((element) => {
            let onLoad = (<HTMLElement>element).dataset["jgwcOnLoad"];
            let segs = onLoad!.split(".");
            let obj = <any>window;
            while (obj && segs.length > 0) {
                obj = obj[<string>segs.shift()];
            }
            if (obj && typeof obj === "function") {
                obj(element, isUpdate);
            } else {
                Log.warn('Specified jgwc-on-load function "' 
                    + onLoad + '" not found.');
            }
        });
    }

    /**
     * Execute unload functions depth first. 
     */
    private _execOnUnload(element: Element, isUpdate: boolean) {
        for (let i = 0; i < element.children.length; i++) {
            if (element.children[i] instanceof HTMLElement) {
                this._execOnUnload(<HTMLElement>element.children[i], isUpdate);
            }
        }
        if (element.hasAttribute("data-jgwc-on-unload")) {
            let onUnload = (<HTMLElement>element).dataset["jgwcOnUnload"];
            let segs = onUnload!.split(".");
            let obj: any = window;
            while (obj && segs.length > 0) {
                obj = obj[<string>segs.shift()];
            }
            if (obj && typeof obj === "function") {
                obj(element, isUpdate);
            } else {
                Log.warn('Specified jgwc-on-unload function "' 
                    + onUnload + '" not found.');
            }
        }
    }

    /**
     * Invokes the functions defined in `data-jgwc-on-action`
     * attributes of the tree with root `container`. Must be 
     * invoked by edit or modal dialogs when they are closed.
     * 
     * @param container the container of the edit dialog
     * @return `true` if the dialog should be closed
     */
    execOnAction(container: HTMLElement, apply: boolean, close: boolean) {
        container.querySelectorAll("[data-jgwc-on-action]").forEach((element) => {
            let onAction = (<HTMLElement>element).dataset["jgwcOnAction"];
            let segs = onAction!.split(".");
            let obj: any = window;
            while (obj && segs.length > 0) {
                obj = obj[<string>segs.shift()];
            }
            if (obj && typeof obj === "function") {
                obj(element, apply, close);
            } else {
                Log.warn('Specified jgwc-on-Action function "' 
                    + onAction + '" not found.');
            }
        });
        if (close) {
            this._execOnUnload(container, false);
        }
    }

    /**
     * Registers a conlet function that is to be invoked when a
     * JSON RPC notification with method `notifyConletView`
     * is received from the server. 
     * 
     * @param conletClass the conlet type for which
     * the method is registered
     * @param functionName the method that is registered
     * @param conletFunction the function to invoke
     */
    registerConletFunction(conletClass: string, functionName: string,
        conletFunction: (conletId: string, ...args: any[]) => void) {
        let classRegistry = this._conletFunctionRegistry.get(conletClass);
        if (!classRegistry) {
            classRegistry = new Map();
            this._conletFunctionRegistry.set(conletClass, classRegistry);
        }
        classRegistry.set(functionName, conletFunction);
    }

    /**
     * Delegates to {@link Renderer.findPreviews}.
     */
    findPreviews() {
        return this._renderer!.findPreviews();
    }

    /**
     * Delegates to {@link Renderer.findConletPreview}.
     */
    findConletPreview(conletId: string) {
        return this._renderer!.findConletPreview(conletId);
    }

    /**
     * Delegates to {@link Renderer.findViews}.
     */
    findViews() {
        return this._renderer!.findViews();
    }

    /**
     * Delegates to {@link Renderer.findConletContents}.
     */
    findConletContents(conletId?: string) {
        return this._renderer!.findConletContents(conletId);
    }

    /**
     * Delegates to {@link Renderer.findConletView}.
     */
    findConletView(conletId: string) {
        return this._renderer!.findConletView(conletId);
    }

    /**
     * Delegates to {@link Renderer.notification}.
     *
     * @return an object representing the notification
     */
    notification(content: string, options: NotificationOptions = {})
            : ConsoleNotification {
        return this._renderer!.notification(content, options);
    }

    /**
     * Delegates to {@link Renderer.updateConletTitle}.
     */
    updateConletTitle(conletId: string, title: string) {
        return this._renderer!.updateConletTitle(conletId, title);
    }

    /**
     * Delegates to {@link Renderer.updateConletModes}.
     */
    updateConletModes(conletId: string, modes: RenderMode[]) {
        return this._renderer!.updateConletModes(conletId, modes);
    }
    
    // Send methods

    /**
     * Invokes the given method on the server.
     *
     * @param the method
     * @param params the parameters
     */
    send(method: string, ...params: any[]) {
        if (params.length > 0) {
            this._webSocket.send({
                "jsonrpc": "2.0", "method": method,
                "params": params
            });
        } else {
            this._webSocket.send({ "jsonrpc": "2.0", "method": method });
        }
    }

    /**
     * Sends a notification for changing the language to the server.
     * 
     * @param locale the id of the selected locale
     */
    setLocale(locale: string, reload: boolean) {
        this.send("setLocale", locale, reload);
    }

    /**
     * Sends a notification that requests the rendering of a conlet.
     * 
     * @param conletId the conlet id
     * @param modes the requested render mode(s)
     */
    renderConlet(conletId: string, modes: RenderMode[]) {
        this.send("renderConlet", conletId, modes);
    }

    /**
     * Sends a notification to the server requesting the 
     * addition of a conlet.
     * 
     * @param conletType the type of the conlet to add
     * @param renderModes the requested render mode(s),
     *      {@link RenderMode.Foreground} is automatically added
     */
    addConlet(conletType: string, renderModes: RenderMode[],
            properties: Map<string,string> | null = null) {
        if (!renderModes) {
            renderModes = [];
        }
        renderModes.push(RenderMode.Foreground);
        if (properties !== null) {
            this.send("addConlet", conletType, renderModes, 
                Object.fromEntries(properties));
        } else {
            this.send("addConlet", conletType, renderModes);
        } 
    }

    /**
     * Removes a conlet preview by invoking the respective methods
     * of the associated renderer. If a view of the conlet is
     * displayed, it will be removed, too.
     *
     * After un-displaying the conlet, any "execOnUnload" functions
     * in the conlet tree are invoked (depth first) and JSON RPC 
     * notifications with method `conletDeleted` and the conlet id 
     * as parameter are sent to the server for nested component 
     * conlets and the preview itself (again depth first). The 
     * notifications for the component conlets have as additional
     * argument the collected properties.  
     *
     * @param the conlet id
     */
    removePreview(conletId: string) {
        let view = this._renderer!.findConletView(conletId);
        let notifications = new Array<Object>();
        let modes: RenderMode[] = [];
        if (view) {
            modes.push(RenderMode.View);
            this._renderer!.removeConletDisplays([view]);
            this._execOnUnload(view.element(), false);
            this._removeEmbedded(notifications, view);
        }
        let preview = this._renderer!.findConletPreview(conletId);
        if (preview) {
            modes.push(RenderMode.Preview);
            this._renderer!.removeConletDisplays([preview]);
            this._execOnUnload(preview.element(), false);
            this._removeEmbedded(notifications, preview);
        }
        notifications.push([conletId, modes]);
        this.send("conletsDeleted", notifications);
    }

    /**
     * Removes a conlet view by invoking the respective methods
     * of the associated renderer.
     *
     * After un-displaying the conlet, any "execOnUnload" functions
     * in the conlet tree are invoked (depth first) and JSON RPC 
     * notifications with method `conletDeleted` and the conlet id 
     * as parameter are sent to the server for nested component 
     * conlets and the view itself (again depth first). The 
     * notifications for the component conlets have as additional
     * argument the collected properties.  
     *
     * @param the conlet id
     */
    removeView(conletId: string) {
        let view = this._renderer!.findConletView(conletId);
        if (!view) {
            return;
        }
        let notifications = new Array<Object>();
        this._renderer!.removeConletDisplays([view]);
        this._execOnUnload(view.element(), false);
        this._removeEmbedded(notifications, view);
        notifications.push([conletId, [RenderMode.View]]);
        this.send("conletsDeleted", notifications);
    }

    private _removeEmbedded(notifications: Array<Object>, conlet: Conlet) {
        for (let embedded of this._renderer!.findEmbeddedConlets(conlet)) {
            // Content may be rendered sveral times for the same conlet
            if (this._renderer!.findConletContents(embedded.id())) {
                continue;
            }
            notifications.push([embedded.id(), [RenderMode.Content], 
                Object.fromEntries(this.collectConletProperties(embedded))]);
        }
    }

    /**
     * Send the current console layout to the server.
     *
     * @param previewLayout the conlet ids from top left
     * to bottom right
     * @param tabsLayout the ids of the conlets viewable in tabs
     * @param xtraInfo extra information spcific to the 
     * console implementation
     */
    updateLayout(previewLayout: string[], tabLayout: string[], xtraInfo: Object) {
        if (!this.configured) {
            return;
        }
        this.send("consoleLayout", previewLayout, tabLayout, xtraInfo);
    }

    /**
     * Sends a JSON RPC notification to the server with method 
     * `notifyConletModel` and as parameters the given `conletId`, 
     * the `method` and the additional arguments. 
     * 
     * @param conletId the id of the conlet to send to
     * @param method the method to invoke
     * @param args additional arguments to send
     */
    notifyConletModel(conletId: string, method: string, ...args: any[]) {
        if (args === undefined) {
            this.send("notifyConletModel", conletId, method);
        } else {
            this.send("notifyConletModel", conletId, method, args);
        }
    }

}

export default Console;
export { PageComponentSpecification };

