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

import JGConsole, { Console, PageComponentSpecification, RenderMode, 
    Notification, NotificationOptions, NotificationType, ModalDialogOptions, 
    parseHtml, Conlet } from "jgconsole";
import { GridStack, GridStackWidget, GridStackElement } from "gridstack";
import { reactive, ref, createApp, onMounted, computed, Ref } from "vue";
import AashPlugin, { provideApi, getApi, AashTablist, 
    AashModalDialogComponent, AashModalDialog } from "aash-plugin";

import "./console.scss"

var log = JGConsole.Log;

export default class Renderer extends JGConsole.Renderer {

    private _lastXtraInfo: any = {};
    private _connectionLostNotification: Notification | null = null;
    private _l10nMessages: Map<string, Map<string,string>>;
    private _localeMenuItems: [[string, string]];
    private _lang: Ref<string>;
    private _conletTypes: Array<[string | (() => string), string, string[]]>;
    private _displayNames: Map<string, Map<string,string>>;
    private _previewGrid: GridStack | null = null;
    private _isConfigured = false;

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
        this._displayNames = new Map<string,Map<string,string>>();
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
        headerApp.use(AashPlugin, []);
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
            draggable: {
                handle: '.ui-draggable-handle' 
            },
            // alwaysShowResizeHandle: 'mobile' // now default 
            // /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        _this._previewGrid = GridStack.init(options, '#consolePreviews');
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
            if (a.getAttribute("gs-y") != b.getAttribute("gs-y")) {
                return +b.getAttribute("gs-y")! - +a.getAttribute("gs-y")!;
            }
            return +b.getAttribute("gs-x")! - +a.getAttribute("gs-x")!;
        });

        let previewLayout:string[] = [];
        let xtraInfo: any = {};
        gridItems.forEach(function(item) {
            let conletId = (<HTMLElement>item.querySelector
                (".conlet-preview[data-conlet-id]")!).dataset["conletId"]!;
            previewLayout.push(conletId);
            xtraInfo[conletId] = [item.getAttribute("gs-x"), 
                item.getAttribute("gs-y"), item.getAttribute("gs-w"),
                item.getAttribute("gs-h")];
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
    }

    addConletType(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[], 
            pageComponents: PageComponentSpecification[]) {
        let _this = this;
        _this._displayNames.set(conletType, displayNames);
        if (renderModes.includes(RenderMode.Preview)
            || renderModes.includes(RenderMode.View)) {
            // Add to menu
            let label = function() {
                return <string>JGConsole.forLang(displayNames, _this.locale()) || "Conlet";
            };
            _this._conletTypes.push([label, conletType, renderModes]);
        }
        // Add embedded to area(s)
        let header = <HTMLElement>document.querySelector("#vuejs-console-header");
        for (let item of pageComponents) {
            if (item.area === "headerIcons") {
                this._embedAsHeaderIcon(header, conletType, item);
            }
        }
    }
    
    private _embedAsHeaderIcon(header: HTMLElement, conletType: string,
             spec: PageComponentSpecification) {
        let conlet = document.createElement("div");
        conlet.setAttribute("class", "conlet conlet-content");
        conlet.dataset["conletType"] = conletType;
        for (let prop in spec.properties) {
            conlet.dataset["conlet" + prop.substring(0,1).toUpperCase()
                + prop.substring(1)] = spec.properties[prop];
        }
        let conletPrio = parseInt(spec.properties["priority"] || "0");
        for (let idx = 0; idx < header.children.length; idx++) {
            let ref = <HTMLElement>header.children.item(idx);
            if (!("conletType" in ref.dataset)) {
                continue;
            }
            let refPrio = parseInt(ref.dataset["conletPriority"] || "0");
            if (conletPrio < refPrio || conletPrio == refPrio 
                    && conletType < ref.dataset["conletType"]!) {
                header.insertBefore(conlet, ref);
                return;
            }
        }
        header.append(conlet);
    }        

    updateConletType(conletType: string, renderModes: RenderMode[]) {
        // Remove from menu
        let _this = this;
        _this._conletTypes.splice(0, _this._conletTypes.length,
            ..._this._conletTypes.filter(el => el[1] != conletType));
        let displayNames = _this._displayNames.get(conletType)!;
        
        // Add to menu
        let label = function() {
            return <string>JGConsole.forLang(displayNames, _this.locale()) || "Conlet";
        };
        _this._conletTypes.push([label, conletType, renderModes]);
    }
    
    conletMenuItems() {
        return this.conletTypes().filter(el => el[2].includes(RenderMode.Preview) 
            || el[2].includes(RenderMode.View));
    }
    
    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
        loaderOverlay.classList.add("loader-overlay_hidden")
        this._isConfigured = true;
    }

    connectionSuspended(resume: () => void) {
        let _this = this;
        let dialog = createApp(AashModalDialogComponent, {
            title: _this.localize("Console Session Suspended"),
            showCancel: false,
            content: _this.localize("consoleSessionSuspendedMessage"),
            contentClasses: [],
            closeLabel: _this.localize("Resume"),
            onAction: function(applyChanges: boolean) {
                dialog.unmount();
                resume();
            }
        });
        let node = dialog.mount(document.querySelector("#suspended-dialog-slot")!)
            .$el;
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

    updateConletPreview(isNew: boolean, conlet: Conlet, 
        modes: RenderMode[], content: HTMLElement[], foreground: boolean) {
        // Container is:
        //     <section class='conlet conlet-preview' data-conlet-id='...' 
        //     data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>"
        let _this = this;
        let container = conlet.element();
        if (isNew) {
            container.append(...parseHtml(
                '<header class="ui-draggable-handle"></header>'
                + '<section></section>'));

            // Get grid info
            let conletId = conlet.id();
            let options: GridStackWidget = {};
            if (conletId in this._lastXtraInfo) {
                options.autoPosition = false;
                options.x = this._lastXtraInfo[conletId][0];
                options.y = this._lastXtraInfo[conletId][1];
                options.w = this._lastXtraInfo[conletId][2];
                options.h = this._lastXtraInfo[conletId][3];
            } else {
                options.autoPosition = true;
                options.w = 4;
                options.h = 4;
                if (content[0].dataset["conletGridColumns"]) {
                    options.w = +content[0].dataset["conletGridColumns"];
                }
                if (content[0].dataset["conletGridRows"]) {
                    options.h = +content[0].dataset["conletGridRows"];
                }
                if (window.innerWidth < 1200) {
                    let winWidth = Math.max(320, window.innerWidth);
                    let width = options.w;
                    width = Math.round(width + (12 - width) 
                        * (1 - (winWidth - 320) / (1200 - 320)));
                    options.w = width;
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
            this._mountHeader(<HTMLElement>conlet.element()
                .querySelector(":scope > header")!, conletId);
        }
        let headerComponent = getApi<any>(container.querySelector(":scope > header"));
        headerComponent.setTitle(_this._evaluateTitle(container, content[0]));
        headerComponent.setModes(modes);
        let previewContent = container.querySelector("section")!;
        while (previewContent.firstChild) {
            previewContent.removeChild(previewContent.lastChild!);
        }
        previewContent.append(...content);
        if (foreground) {
            this._consoleTabs().selectPanel("consoleOverviewPanel");
        }
    }

    _mountHeader(header: HTMLElement, conletId: string) {
        let _this = this;
        createApp({
            template: `
              <p>{{ evalTitle }}</p>
              <button v-if="hasHelp"
                type='button' class='fa fa-question-circle-o' @click="showHelp()"
              ></button><button v-if="isEditable"
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

                const hasHelp = computed(() => {
                    return modes.includes(RenderMode.Help);
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

                const showHelp = () => {
                    _this.console.renderConlet(
                        conletId, [RenderMode.Help, RenderMode.Foreground]);
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
                    hasHelp, showHelp, hasView, edit, removePreview, showView, 
                    header }
            }
        }).mount(header);
    }

    updateConletView(isNew: boolean, conlet: Conlet, 
        modes: string[], content: HTMLElement[], foreground: boolean) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        let _this = this;
        let conletId = conlet.id();
        let panelId = "conlet-panel-" + conletId;
        let container = conlet.element();
        if (isNew) {
            container.setAttribute("id", panelId);
            container.setAttribute("hidden", "");
            container.append(...content);
            let consolePanels = <HTMLElement>document.querySelector("#consolePanels");
            consolePanels.append(container);
            // Add to tab list
            this._consoleTabs().addPanel({
                id: panelId, 
                label: _this._evaluateTitle(container, content[0]), 
                removeCallback: function(): void { _this.console.removeView(conletId!); }
            });
            this._layoutChanged();
        } else {
            while (container.firstChild) {
                container.removeChild(container.lastChild!);
            }
            container.append(...content);
            for (let panel of this._consoleTabs().panels()) {
                if (panel.id === panelId) {
                    panel.label = _this._evaluateTitle(container, content[0]);
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
    
    removeConletDisplays(conlets: Conlet[]) {
        let _this = this;
        conlets.forEach(function(conlet) {
            if (conlet.isPreview()) {
                let gridItem = conlet.element().closest(".grid-stack-item");
                _this._previewGrid!.removeWidget(<GridStackElement>gridItem);
            }
            if (conlet.isView()) {
                let panelId = conlet.element().getAttribute("id")!;
                _this._consoleTabs().removePanel(panelId);
                conlet.element().remove();
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
            let conletHeader = <HTMLElement>preview.element()
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
        let headerComponent = getApi<any>(conlet.element()
            .querySelector(":scope > header"));
        headerComponent.setModes(modes);
    }

    openModalDialog(container: HTMLElement, options: ModalDialogOptions,
        content: string) {
        const _this = this;
        const formId = container.id! + "-form";
        const dialog = createApp (AashModalDialogComponent, {
            content: content,
            title: options.title,
            showCancel: options.cancelable || false,
            applyLabel: options.applyLabel || "",
            okayLabel: options.okayLabel || "",
            submitForm: options.useSubmit ? formId : null,
            onAction: function(apply: boolean, close: boolean) {
                _this.console.execOnAction(container, apply, close);
                if (close) {
                    dialog.unmount();
                }
            }
        });
        
        let dialogEl = dialog.mount(container).$el;
        let dialogApi = getApi<AashModalDialog.Api>(dialogEl)!;
        dialogApi.open();
        if (!this._isConfigured) {
            let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
            loaderOverlay.classList.add("loader-overlay_hidden")
        }
    }
    
    closeModalDialog(container: HTMLElement) {
        if (!this._isConfigured) {
            let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
            loaderOverlay.classList.remove("loader-overlay_hidden")
        }
        let dialogApi = getApi<AashModalDialog.Api>
            (<HTMLElement>container.firstChild!)!;
        dialogApi.cancel();
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
