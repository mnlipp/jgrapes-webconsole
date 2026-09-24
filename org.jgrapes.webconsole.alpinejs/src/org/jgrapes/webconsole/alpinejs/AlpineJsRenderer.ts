/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026  Michael N. Lipp
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
import { AashTablist, AashDropdownMenu, AashModalDialog } from "aash-alpinejs";
import Alpine from "alpinejs";

import "./console.scss"

var log = JGConsole.Log;

interface ConletType {
    type: string;
    renderModes: RenderMode[];
    displayNames: Map<string,string>;
}

interface ConsoleData {
    locale: string;
    localeMenuItems: [[string, string]];
    conletTypes: Array<ConletType>;
}

export default class AlpineJsRenderer extends JGConsole.Renderer {

    private lastXtraInfo: any = {};
    private connectionLostNotification: Notification | null = null;
    private l10nMessages: Map<string, Map<string,string>>;
    private displayNames: Map<string, Map<string,string>>;
    private previewGrid: GridStack | null = null;
    private isConfigured = false;
    private consoleData: ConsoleData;

    constructor(console: Console, localeMenuItems: [[string, string]], 
            l10nMessages: Map<string, Map<string,string>>) {
        super(console);
        this.l10nMessages = l10nMessages;
        let htmlRoot = document.querySelector("html")!;
        this.consoleData = Alpine.reactive({
            locale: htmlRoot.getAttribute('lang') || 'en',
            localeMenuItems: localeMenuItems,
            conletTypes: []
        });
        Alpine.magic('consoleRenderer', () => this);
        let _this = this;
        
        /*
         * Install a MutationObserver for root html node's attribute lang
         * that feeds back the value to a the lang property
         * in app data for simpler reactivity.
         */
        new MutationObserver(function(mutations) {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes'
                    && mutation.attributeName === 'lang') {
                    let newLang = htmlRoot.getAttribute('lang') || 'en';
                    _this.consoleData.locale = newLang;
                    _this.console.setLocale(newLang, false);
                }
            });
        }).observe(htmlRoot, {
          childList: false,
          attributes: true,
          subtree: false
        });

        // Shared state
        this.displayNames = new Map<string,Map<string,string>>();
    }
    
    _consoleTabs() {
        return (<AashTablist>document.querySelector("#consoleTabs"))!;
    }
    
    init() {
        let _this = this;
        log.debug("Locking screen");
        
        // Init tabs
        const overviewTpl = document.createElement("template");
        overviewTpl.setAttribute("provides", "tab");
        overviewTpl.setAttribute("panel-id", "consoleOverviewPanel");
        overviewTpl.innerHTML = `<span>${_this.localize("Overview")}</span>`;
        this._consoleTabs().appendChild(overviewTpl);

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
        _this.previewGrid = GridStack.init(options, '#consolePreviews');
        if (!_this.previewGrid) {
            log.error("VueJsConsole: Creating preview grid failed.")
        }
        document.querySelector('#consolePreviews')!.addEventListener("change",
            () => { _this._layoutChanged(); });
    }

    locale() {
        return this.consoleData.locale;
    }

/*    setLocale(lang: string) {
        document.querySelector("html")!.setAttribute('lang', lang);
        this.console.setLocale(lang, false);
        
    }
*/
    localeMenuItems() {
        return this.consoleData.localeMenuItems;
    }

    localize(key: string) {
        return JGConsole.localize(this.l10nMessages, this.locale(), key);
    }
    
    _layoutChanged() {
        let gridItems: HTMLElement[] = [];
        document.querySelectorAll("#consolePreviews .grid-stack-item")
            .forEach((element) => {
                gridItems.push(<HTMLElement>element);
        });
        gridItems.sort(function(a: HTMLElement, b: HTMLElement) {
            if (+a.getAttribute("gs-y")! != +b.getAttribute("gs-y")!) {
                return +a.getAttribute("gs-y")! - +b.getAttribute("gs-y")!;
            }
            return +a.getAttribute("gs-x")! - +b.getAttribute("gs-x")!;
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
        const tabTemplates = this._consoleTabs().querySelectorAll(
            '[provides="tab"]');
        tabTemplates.forEach(tpl => {
            const panelId = tpl.getAttribute('panel-id');
            if (panelId) {
                let tabpanel = <HTMLElement>document
                    .querySelector("[id='" + panelId + "']");
                if (tabpanel) {
                    let conletId = tabpanel.dataset["conletId"];
                    if (conletId) {
                        tabsLayout.push(conletId);
                    }
                }
            }
        });
        this.console.updateLayout(previewLayout, tabsLayout, xtraInfo);
    }

    addConletType(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[], 
            pageComponents: PageComponentSpecification[]) {
        let _this = this;
        _this.displayNames.set(conletType, displayNames);
        if (renderModes.includes(RenderMode.Preview)
            || renderModes.includes(RenderMode.View)) {
            // Add to menu
            _this.consoleData.conletTypes.push({ type: conletType,
                renderModes, displayNames} satisfies ConletType);
        }
        // Add embedded to area(s)
        let header = <HTMLElement>document.querySelector("#ajs-console-header");
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
        const conletTypes = _this.consoleData.conletTypes;
        conletTypes.splice(0, conletTypes.length,
            ...conletTypes.filter(el => el.type != conletType));
        let displayNames = _this.displayNames.get(conletType)!;
        
        // Add to menu
        _this.consoleData.conletTypes.push({type: conletType,
            renderModes, displayNames} satisfies ConletType);
    }
    
    conletTypes() {
        return this.consoleData.conletTypes;
    }
    
    conletMenuItems() {
        const _this = this;
        const list = this.consoleData.conletTypes.filter(
            el => el.renderModes.includes(RenderMode.Preview)
                || el.renderModes.includes(RenderMode.View));
        const locale = _this.locale();
        list.sort((a, b) => {
            const nameA = <string>JGConsole.forLang(a.displayNames, locale) || "";
            const nameB = <string>JGConsole.forLang(b.displayNames, locale) || "";
            return nameA.localeCompare(nameB, locale);
        });
        return list;
    }
    
    displayName(conletType: string): string | null {
        const displayNames = this.displayNames.get(conletType)!;
        return <string>JGConsole.forLang(
            displayNames, this.locale()) || "Conlet"
    }

    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
        loaderOverlay.classList.add("loader-overlay_hidden")
        this.isConfigured = true;
    }

    connectionSuspended(resume: () => void) {
        let _this = this;
        const dialog = document
            .querySelector("#suspended-dialog")! as AashModalDialog;
        dialog.action = function(_apply: boolean, close: boolean) {
            if (close) {
                resume();
            }
        };
        dialog.open();
    }

    connectionLost() {
        let _this = this;
        if (this.connectionLostNotification == null) {
            this.connectionLostNotification =
                this.notification(
                    _this.localize("serverConnectionLostMessage"), {
                    type: NotificationType.Error,
                    closeable: false,
                });
        }
    }

    connectionRestored() {
        if (this.connectionLostNotification != null) {
            this.connectionLostNotification.close();
        }
        this.notification(this.localize("serverConnectionRestoredMessage"), {
            type: NotificationType.Success,
            autoClose: 2000,
        });
    }

    lastConsoleLayout(previewLayout: string[], tabsLayout: string[],
            xtraInfo: Object) {
        this.lastXtraInfo = xtraInfo;
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
            if (conletId in this.lastXtraInfo) {
                options.autoPosition = false;
                options.x = +this.lastXtraInfo[conletId][0];
                options.y = +this.lastXtraInfo[conletId][1];
                options.w = +this.lastXtraInfo[conletId][2];
                options.h = +this.lastXtraInfo[conletId][3];
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
                ('<div class="grid-stack-item" role="gridcell"></div>')[0];
            container.classList.add('grid-stack-item-content');
            gridItem.append(container);

            // Finally add to grid
            this.previewGrid!.makeWidget(gridItem, options);
            this._layoutChanged();

            // Generate header
            this._mountHeader(<HTMLElement>conlet.element()
                .querySelector(":scope > header")!, conletId);
        }
/*        let headerComponent = getApi<any>(container.querySelector(":scope > header"));
        headerComponent.setTitle(_this._evaluateTitle(container, content[0]));
        headerComponent.setModes(modes);
*/
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

/*                provideApi (header, {
                    setTitle, isEditable, isRemovable,
                    hasView, edit, removePreview, showView,
                    setModes: (newModes: RenderMode[]) => { 
                        modes.length = 0;
                        modes.push(...newModes);
                    }
                });
*/
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
            const tabTpl = document.createElement("template");
            tabTpl.setAttribute("provides", "tab");
            tabTpl.setAttribute("panel-id", panelId);
            tabTpl.setAttribute("remove-callback",
                `_this.console.removeView('${conletId}');
                _consoleTabs().removePanel('${panelId}')`);
            tabTpl.innerHTML = `<span>${_this._evaluateTitle(container, content[0])}</span>`;
            this._consoleTabs().appendChild(tabTpl);
            this._layoutChanged();
        } else {
            while (container.firstChild) {
                container.removeChild(container.lastChild!);
            }
            container.append(...content);
            const tabTpl = <HTMLElement>this._consoleTabs()
                .querySelector(`[panel-id="${panelId}"]`);
            if (tabTpl) {
                tabTpl.innerHTML = `<span>${_this._evaluateTitle(container, content[0])}</span>`;
            }
        }
        if (foreground) {
            this._consoleTabs().selectPanel(panelId);
        }
    }

    _evaluateTitle(container: HTMLElement, content: HTMLElement): string {
        let title: string | null | undefined = content.dataset["conletTitle"];
        if (!title) {
            let conletType = container.dataset["conletType"];
            if (conletType) {
                title = this.displayName(conletType);
            }
        }
        return title || "(Untitled)";
    }
    
    removeConletDisplays(conlets: Conlet[]) {
        let _this = this;
        conlets.forEach(function(conlet) {
            if (conlet.isPreview()) {
                let gridItem = conlet.element().closest(".grid-stack-item");
                _this.previewGrid!.removeWidget(<GridStackElement>gridItem);
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
/*            let headerComponent = getApi<any>(conletHeader);
            headerComponent.setTitle(title);
 */        }
        const tabTpl = <HTMLElement>this._consoleTabs()
            .querySelector(`[panel-id="conlet-panel-${conletId}"]`);
        if (tabTpl) {
            tabTpl.innerHTML = `<span>${title}</span>`;
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
/*        let headerComponent = getApi<any>(conlet.element()
            .querySelector(":scope > header"));
        headerComponent.setModes(modes);
*/    }

    openModalDialog(container: HTMLElement, options: ModalDialogOptions,
        content: string) {
        const _this = this;
        const formId = container.id! + "-form";
        let dialog = <AashModalDialog>document.createElement("aash-modal-dialog");
        dialog.submitForm = options.useSubmit ? formId : null;
        dialog.onAction = function(apply: boolean, close: boolean) {
            _this.console.execOnAction(container, apply, close);
            if (close) {
                dialog.remove();
            }
        };
        if (options.title) {
            const titleTpl = document.createElement("template");
            titleTpl.setAttribute("provides", "dialog-title");
            titleTpl.innerHTML = `<p>${options.title}</p>`;
            dialog.appendChild(titleTpl);
        }
        if (options.cancelable) {
            const cancelTpl = document.createElement("template");
            cancelTpl.setAttribute("provides", "cancel-label");
            cancelTpl.innerHTML = `<span class="fa fa-times"></span>`;
            dialog.appendChild(cancelTpl);
        }
        if (options.applyLabel) {
            const applyTpl = document.createElement("template");
            applyTpl.setAttribute("provides", "apply-label");
            applyTpl.innerHTML = `<span>${options.applyLabel}</span>`;
            dialog.appendChild(applyTpl);
        }
        if (options.okayLabel) {
            const okayTpl = document.createElement("template");
            okayTpl.setAttribute("provides", "okay-label");
            okayTpl.innerHTML = `<span>${options.okayLabel}</span>`;
            dialog.appendChild(okayTpl);
        }
        const contentTpl = document.createElement("template");
        contentTpl.setAttribute("provides", "content");
        contentTpl.innerHTML = content;
        dialog.appendChild(contentTpl);
        container.appendChild(dialog);
        dialog.open();
        if (!this.isConfigured) {
            let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
            loaderOverlay.classList.add("loader-overlay_hidden")
        }
    }
    
    closeModalDialog(container: HTMLElement) {
        if (!this.isConfigured) {
            let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
            loaderOverlay.classList.remove("loader-overlay_hidden")
        }
        let dialog = <AashModalDialog>container.firstChild!;
        dialog.cancel();
        dialog.remove();
    }
    
/*    notification(content: string, options: NotificationOptions): Notification {
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

*/}
