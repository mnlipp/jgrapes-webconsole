/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2021  Michael N. Lipp
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

/// <reference path="../../../../../../org.jgrapes.webconsole.provider.gridstack/node_modules/gridstack/dist/gridstack.d.ts" />

import JGConsole, { Console, RenderMode, Notification, NotificationOptions,
    NotificationType, ModalDialogOptions, parseHtml } from "@JGConsole"
import { reactive, ref, createApp, onMounted, computed, Ref } from "@Vue";
import AashPlugin, { provideApi, getApi, AashTablist, 
    AashModalDialogComponent, AashModalDialog } from "@Aash";

import "./console.scss"

var log = JGConsole.Log;

export default class Renderer extends JGConsole.Renderer {

    private _lastXtraInfo: any = {};
    private _connectionLostNotification: Notification | null = null;
    private _l10nMessages: Map<string, Map<string,string>>;
    private _localeMenuItems: [[string, string]];
    private _lang: Ref<string>;
    private _conletTypes: Array<[string | (() => string), string, string[]]>;
    private _previewGrid: GridStack | null = null;

    constructor(console: Console, localeMenuitems: [[string, string]], 
            l10nMessages: Map<string, Map<string,string>>) {
        super(console);
        let _this = this;
        
        // Prepare console i18n
        this._l10nMessages = l10nMessages;
        // window.consoleL10n = function(key) { return _this.localize(key) };
        
        // Shared state
        this._localeMenuItems = reactive(localeMenuitems);
        this._lang = ref(document.querySelector("html")!
            .getAttribute('lang') || 'en');
        this._conletTypes = reactive<Array<[string, string, [string]]>>([]);
    }
    
    _consoleTabs() {
        return getApi<AashTablist.Api>
            (<HTMLElement>document.querySelector("#consoleTabs"))!;
    }
    
    init() {
        let _this = this;
        log.debug("Locking screen");
        
        // Start Vue
        let headerApp = createApp({});
        headerApp.use(AashPlugin);
        headerApp.config.globalProperties.window = window;
        headerApp.config.globalProperties.$consoleRenderer = this;
        headerApp.mount('header');

        let consoleVueApp = createApp({});
        consoleVueApp.use(AashPlugin);
        consoleVueApp.config.globalProperties.window = window;
        consoleVueApp.config.globalProperties.$consoleRenderer = this;
        consoleVueApp.mount('.consoleVue');

        // Init tabs
        this._consoleTabs().addPanel({  
            id: "consoleOverviewPanel",
            label: function() { return _this.localize("Overview") }
        });

        // Grid
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            alwaysShowResizeHandle: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        _this._previewGrid = window.GridStack.init(options, '#consolePreviews');
        if (!_this._previewGrid) {
            log.error("VueJsConsole: Creating preview grid failed.")
        }
        document.querySelector('#consolePreviews')!.addEventListener("change",
            () => { _this._layoutChanged(); });
    }

    locale() {
        return this._lang.value;
    }

    setLocale(lang: string) {
        this._lang.value = lang;
        document.querySelector("html")!.setAttribute('lang', lang);
        this.console.setLocale(lang, false);
        
    }

    localeMenuItems() {
        return this._localeMenuItems;
    }

    conletTypes() {
        return this._conletTypes;
    }

    localize(key: string) {
        return JGConsole.localize(this._l10nMessages, 
            this.locale(), key);
    }
    
    _layoutChanged() {
        let gridItems: HTMLElement[] = [];
        document.querySelectorAll("#consolePreviews .grid-stack-item")
            .forEach((element) => {
                gridItems.push(<HTMLElement>element);
        });
        gridItems.sort(function(a: HTMLElement, b: HTMLElement) {
            if (a.dataset["gsY"] != b.dataset["gsY"]) {
                return +b.dataset["gsY"]! - +a.dataset["gsY"]!;
            }
            return +b.dataset["gsX"]! - +a.dataset["gsX"]!;
        });

        let previewLayout:string[] = [];
        let xtraInfo: any = {};
        gridItems.forEach(function(item) {
            let conletId = (<HTMLElement>item.querySelector
                (".conlet-preview[data-conlet-id]")!).dataset["conletId"]!;
            previewLayout.push(conletId);
            xtraInfo[conletId] = [item.dataset["gsX"],
            item.dataset["gsY"], item.dataset["gsWidth"],
            item.dataset["gsHeight"]]
        });

        let tabsLayout: string[] = [];
        for (let panel of this._consoleTabs().panels()) {
            let tabpanel = <HTMLElement>document
                .querySelector("[id='" + panel.id + "']");
            if (tabpanel) {
                let conletId = tabpanel.dataset["conletId"];
                if (conletId) {
                    tabsLayout.push(conletId);
                }
            }
        }
        this.console.updateLayout(previewLayout, tabsLayout, xtraInfo);
    };

    addConletType(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[]) {
        let _this = this;
        if (!renderModes.includes(RenderMode.Preview)
            && !renderModes.includes(RenderMode.View)) {
            return;
        }
        // Add to menu
        let label = function() {
            return <string>JGConsole.forLang(displayNames, _this.locale()) || "Conlet";
        };
        _this._conletTypes.push([label, conletType, renderModes]);
    }

    removeConletType(conletType: string) {
        // Remove from menu
        let _this = this;
        _this._conletTypes.splice(0, _this._conletTypes.length,
            ..._this._conletTypes.filter(el => el[1] != conletType));             
    }
    
    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
        loaderOverlay.classList.add("loader-overlay_hidden")
    }

    connectionSuspended(resume: () => void) {
        let _this = this;
        let dialog = createApp(AashModalDialogComponent, {
            title: _this.localize("Console Session Suspended"),
            showCancel: false,
            content: _this.localize("consoleSessionSuspendedMessage"),
            contentClasses: [],
            closeLabel: _this.localize("Resume"),
            onClose: function(applyChanges: boolean) {
                dialog.unmount();
                resume();
            }
        });
        let node = dialog.mount(document.querySelector("#modal-dialog-slot")!)
            .$el.firstChild;
        getApi<AashModalDialog.Api>(node)!.open();
    }

    connectionLost() {
        let _this = this;
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                this.notification(
                    _this.localize("serverConnectionLostMessage"), {
                    type: NotificationType.Error,
                    closeable: false,
                });
        }
    }

    connectionRestored() {
        if (this._connectionLostNotification != null) {
            this._connectionLostNotification.close();
        }
        this.notification(this.localize("serverConnectionRestoredMessage"), {
            type: NotificationType.Success,
            autoClose: 2000,
        });
    }

    lastConsoleLayout(previewLayout: string[], tabsLayout: string[],
            xtraInfo: Object) {
        this._lastXtraInfo = xtraInfo;
    }

    updateConletPreview(isNew: boolean, container: HTMLElement, 
        modes: RenderMode[], content: string, foreground: boolean) {
        // Container is:
        //     <section class='conlet conlet-preview' data-conlet-id='...' 
        //     data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>"
        let _this = this;
        let newContent = parseHtml(content)[0];
        if (isNew) {
            container.append(...parseHtml(
                '<header class="ui-draggable-handle"></header>'
                + '<section class="conlet-content"></section>'));

            // Get grid info
            let conletId = container.dataset["conletId"]!;
            let options: GridstackWidget = {};
            if (conletId in this._lastXtraInfo) {
                options.autoPosition = false;
                options.x = this._lastXtraInfo[conletId][0];
                options.y = this._lastXtraInfo[conletId][1];
                options.width = this._lastXtraInfo[conletId][2];
                options.height = this._lastXtraInfo[conletId][3];
            } else {
                options.autoPosition = true;
                options.width = 4;
                options.height = 4;
                if (newContent.dataset["conletGridColumns"]) {
                    options.width = +newContent.dataset["conletGridColumns"];
                }
                if (newContent.dataset["conletGridRows"]) {
                    options.height = +newContent.dataset["conletGridRows"];
                }
                if (window.innerWidth < 1200) {
                    let winWidth = Math.max(320, window.innerWidth);
                    let width = options.width;
                    width = Math.round(width + (12 - width) 
                        * (1 - (winWidth - 320) / (1200 - 320)));
                    options.width = width;
                }
            }

            // Put into grid item wrapper
            let gridItem = parseHtml
                ('<div class="grid-stack-item" data-gs-auto-position="1"'
                   + ' role="gridcell"></div>')[0];
            container.classList.add('grid-stack-item-content');
            gridItem.append(container);

            // Finally add to grid
            this._previewGrid!.addWidget(gridItem, options);
            this._layoutChanged();

            // Generate header
            this._mountHeader(<HTMLElement>container
                .querySelector(":scope > header")!, conletId);
        }
        let headerComponent = getApi<any>(container.querySelector(":scope > header"));
        headerComponent.setTitle(_this._evaluateTitle(container, newContent));
        headerComponent.setModes(modes);
        let previewContent = container.querySelector("section")!;
        while (previewContent.firstChild) {
            previewContent.removeChild(previewContent.lastChild!);
        }
        previewContent.append(newContent);
        if (foreground) {
            this._consoleTabs().selectPanel("consoleOverviewPanel");
        }
    }

    _mountHeader(header: HTMLElement, conletId: string) {
        let _this = this;
        createApp({
            template: `
              <p>{{ evalTitle }}</p>
              <button v-if="isEditable"
                type='button' class='fa fa-wrench' @click="edit()"
              ></button><button v-if="isRemovable" 
                type="button" class="fa fa-times" @click="removePreview()"
              ></button><button v-if="hasView"
                type="button" class="fa fa-expand" @click="showView()"
              ></button>`,
            setup() {
                const title = ref("");
                const modes: RenderMode[] = reactive([]);

                const setTitle = (value: string) => {
                    title.value = value;
                }
                
                const evalTitle = computed(() => {
                    if (typeof title.value === 'function') {
                        return (<() => string>title.value)();
                    }
                    return title.value;
                });

                const isEditable = computed(() => {
                    return modes.includes(RenderMode.Edit);
                });

                const isRemovable = computed(() => {
                    return !modes.includes(RenderMode.StickyPreview);
                });

                const hasView = computed(() => {
                    return modes.includes(RenderMode.View);
                });
                
                const edit = () => {
                    _this.console.renderConlet(
                        conletId, [RenderMode.Edit, RenderMode.Foreground]);
                };
                
                const removePreview = () => {
                    _this.console.removePreview(conletId)
                };
                
                const showView = () => {
                    _this.console.renderConlet(
                        conletId, [RenderMode.View, RenderMode.Foreground]);
                };

                provideApi (header, {
                    setTitle, isEditable, isRemovable,
                    hasView, edit, removePreview, showView,
                    setModes: (newModes: RenderMode[]) => { 
                        modes.length = 0;
                        modes.push(...newModes);
                    }
                });

                return { conletId, evalTitle, isEditable, isRemovable,
                    hasView, edit, removePreview, showView, header }
            }
        }).mount(header);
    }

    updateConletView(isNew: boolean, container: HTMLElement, 
        modes: string[], content: string, foreground: boolean) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        let _this = this;
        let newContent = parseHtml(content);
        let conletId = container.dataset["conletId"];
        let panelId = "conlet-panel-" + conletId;
        if (isNew) {
            container.setAttribute("id", panelId);
            container.setAttribute("hidden", "");
            container.append(...newContent);
            let consolePanels = <HTMLElement>document.querySelector("#consolePanels");
            consolePanels.append(container);
            // Add to tab list
            this._consoleTabs().addPanel({
                id: panelId, 
                label: _this._evaluateTitle(container, newContent[0]), 
                removeCallback: function(): void { _this.console.removeView(conletId!); }
            });
            this._layoutChanged();
        } else {
            while (container.firstChild) {
                container.removeChild(container.lastChild!);
            }
            container.append(...newContent);
            for (let panel of this._consoleTabs().panels()) {
                if (panel.id === panelId) {
                    panel.label = _this._evaluateTitle(container, newContent[0]);
                }
            }
        }
        if (foreground) {
            this._consoleTabs().selectPanel(panelId);
        }
    }

    _evaluateTitle(container: HTMLElement, content: HTMLElement): string {
        let title = content.dataset["conletTitle"];
        if (!title) {
            let conletType = container.dataset["conletType"];
            for (let item of this.conletTypes()) {
                if (item[1] === conletType) {
                    title = <string>item[0];
                    break;
                }
            }
        }
        return title || "(Untitled)";
    }
    
    removeConletDisplays(containers: HTMLElement[]) {
        let _this = this;
        containers.forEach(function(container) {
            if (container.classList.contains('conlet-preview')) {
                let gridItem = container.closest(".grid-stack-item");
                _this._previewGrid!.removeWidget(<GridStackElement>gridItem);
            }
            if (container.classList.contains('conlet-view')) {
                let panelId = container.getAttribute("id")!;
                _this._consoleTabs().removePanel(panelId);
                container.remove();
                _this._layoutChanged();
            }
        });
        this._consoleTabs().selectPanel("consoleOverviewPanel");
        this._layoutChanged();
    }

    /**
     * Update the title of the conlet with the given id.
     *
     * @param conletId the conlet id
     * @param title the new title
     */
    updateConletTitle(conletId: string, title: string) {
        let preview = this.findConletPreview(conletId);
        if (preview) {
            let conletHeader = <HTMLElement>preview
                .querySelector("section:first-child > header")!;
            let headerComponent = getApi<any>(conletHeader);
            headerComponent.setTitle(title);
        }
        for (let panel of this._consoleTabs().panels()) {
            if (panel.id === "conlet-panel-" + conletId) {
                panel.label = title;
            }
        }
    }

    /**
     * Update the modes of the conlet with the given id.
     * 
     * @param conletId the conlet id
     * @param modes the modes
     */
    updateConletModes(conletId: string, modes: RenderMode[]) {
        let conlet = this.findConletPreview(conletId);
        if (!conlet) {
            return;
        }
        let headerComponent = getApi<any>(conlet.querySelector(":scope > header"));
        headerComponent.setModes(modes);
    }

    openModalDialog(container: HTMLElement, options: ModalDialogOptions,
        content: string) {
        let _this = this;
        let dialog = createApp (AashModalDialogComponent, {
            content: content,
            title: options.title,
            showCancel: options.cancelable || false,
            closeLabel: options.closeLabel,
            onClose: function(applyChanges: boolean) {
                if (applyChanges) {
                    _this.console.execOnApply(container);
                }
                dialog.unmount();
            }
        });
        
        let dialogEl = dialog.mount(container).$el;
        let dialogApi = getApi<AashModalDialog.Api>(dialogEl.firstChild)!;
        dialogApi.open();
    }
    
    notification(content: string, options: NotificationOptions): Notification {
        let notificationArea = document.querySelector("#notification-area")!;
        let notificationNode = document.createElement("div");
        notificationArea.appendChild(notificationNode);
        let notification = createApp({
            template: `
                <div role="alert" :class="notificationClass" ref="alert">
                  <p v-html="content"></p>
                  <button v-if="closeable" type="button" class="fa fa-times"
                    v-on:click="close()"></button>
                </div>
            `,
            setup() {
                const type = ('type' in options) ? options.type : "info";
                const closeable = ('closeable' in options) ? options.closeable : true;
                const autoClose = ('autoClose' in options) ? options.autoClose : false;
                const notificationClass = (() => {
                    if (type == NotificationType.Error) {
                        return "notification--error"
                    }
                    if (type == NotificationType.Success) {
                        return "notification--success";
                    }
                    if (type == NotificationType.Warning) {
                        return "notification--warning";
                    }
                    if (type == NotificationType.Danger) {
                        return "notification--danger";
                    }
                    return "notification--info";
                })();
                
                const close = () => {
                    notification.unmount();
                    notificationNode.remove();
                }
                
                onMounted(() => {
                    if (autoClose) {
                        setTimeout(close, autoClose);
                    }
                });

                const alert = ref(null);
                provideApi(alert, { close });
                
                return { content, notificationClass, closeable, close, alert };
            }
        });
        return getApi<Notification>(notification.mount(notificationNode).$el)!;
    }

}
