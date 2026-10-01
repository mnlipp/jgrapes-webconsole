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
import { AashTablist, AashDropdownMenu, AashModalDialog,
    createFragment, aashId } from "aash-alpinejs";
import Alpine from "alpinejs";
import { ConletTypeManager, ConletType } from "./ConletTypeManager"
import { ConletManager } from "./ConletManager"
import { NotificationManager } from "./NotificationManager"

import "./console.scss"

var log = JGConsole.Log;

interface ConsoleData {
    locale: string;
    localeMenuItems: [[string, string]]
}

export default class AlpineJsRenderer extends JGConsole.Renderer {

    private connectionLostNotification: Notification | null = null;
    private l10nMessages: Map<string, Map<string,string>>;
    private isConfigured = false;
    private consoleData: ConsoleData;
    private conletTypeManager: ConletTypeManager;
    private conletManager: ConletManager;
    private notificationManager: NotificationManager;
    private awaitedTokens: string[] = [];

    constructor(console: Console, localeMenuItems: [[string, string]], 
            l10nMessages: Map<string, Map<string,string>>) {
        super(console);
        this.l10nMessages = l10nMessages;
        let htmlRoot = document.querySelector("html")!;
        this.consoleData = Alpine.reactive({
            locale: htmlRoot.getAttribute('lang') || 'en',
            localeMenuItems: localeMenuItems
        });
        Alpine.magic('consoleRenderer', () => this);
        let _this = this;
        
        this.conletTypeManager = new ConletTypeManager(this);
        this.conletManager = new ConletManager(this, console);
        this.notificationManager = new NotificationManager(this, console);
        
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
    }
    
    init() {
        let _this = this;
        log.debug("Locking screen");
        
        this.conletMgr.init();
        
        document.querySelector('#consolePreviews')!.addEventListener("change",
            () => { _this.conletMgr.layoutChanged(); });
    }

    consoleConfigured() {
        this.conletMgr.layoutChanged();
        log.debug("Unlocking screen");
        let loaderOverlay = <HTMLElement>document.querySelector("#loader-overlay");
        loaderOverlay.classList.add("loader-overlay_hidden")
        this.isConfigured = true;
    }

    awaitInDom(element: HTMLElement) {
        var attributed: HTMLElement = element;
        if (element instanceof HTMLTemplateElement) {
            attributed = element.content.firstElementChild! as HTMLElement;
        }
        const token = aashId();
        attributed.dataset["domCreatedToken"] = token;
        this.awaitedTokens.push(token);
    }
    
    whenRendered(todo: () => void) {
        while (this.awaitedTokens.length > 0) {
            const token = this.awaitedTokens[0];
            const attributed = document.querySelector(
                `[data-dom-created-token="${token}"]`) as HTMLElement;
            if (attributed) {
                this.awaitedTokens.splice(0, 1);
                delete attributed.dataset["domCreatedToken"];
            } else {
                setTimeout(() => this.whenRendered(todo), 0);
                return;
            }
        }
        todo();
    }
    
    get conletTypeMgr() {
        return this.conletTypeManager;
    }
    
    addConletType(conletType: string, displayNames: Map<string,string>,
        renderModes: RenderMode[], 
        pageComponents: PageComponentSpecification[]) {
        this.conletTypeMgr.add(conletType, displayNames, renderModes,
            pageComponents);
    }

    updateConletType(conletType: string, renderModes: RenderMode[]) {
        this.conletTypeMgr.update(conletType, renderModes);
    }

    get conletMgr() {
        return this.conletManager;
    }

    lastConsoleLayout(previewLayout: string[], tabsLayout: string[],
            xtraInfo: Object) {
        this.conletMgr.lastConsoleLayout(previewLayout, tabsLayout, xtraInfo);
    }
    
    updateConletPreview(isNew: boolean, conlet: Conlet, 
        modes: RenderMode[], content: HTMLElement[], foreground: boolean) {
        this.conletMgr.updatePreview(isNew, conlet, modes, content, foreground);
    }

    updateConletView(isNew: boolean, conlet: Conlet, 
        modes: string[], content: HTMLElement[], foreground: boolean) {
        this.conletMgr.updateView(isNew, conlet, modes, content, foreground);
    }
    
    removeConletDisplays(conlets: Conlet[]) {
        this.conletMgr.remove(conlets);
    }

    /**
     * Update the title of the conlet with the given id.
     *
     * @param conletId the conlet id
     * @param title the new title
     */
    updateConletTitle(conletId: string, title: string) {
        this.conletMgr.updateTitle(conletId, title);
    }

    /**
     * Update the modes of the conlet with the given id.
     * 
     * @param conletId the conlet id
     * @param modes the modes
     */
    updateConletModes(conletId: string, modes: RenderMode[]) {
        this.conletMgr.updateModes(conletId, modes);
    }

    openModalDialog(container: HTMLElement, options: ModalDialogOptions,
        content: string) {
        const _this = this;
        const formId = container.id! + "-form";
        let dialog = <AashModalDialog>document.createElement("aash2-modal-dialog");
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
        const contentTpl
            = document.createElement("template") as HTMLTemplateElement;
        contentTpl.setAttribute("provides", "content");
        contentTpl.innerHTML = content;
        this.awaitInDom(contentTpl);
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

    locale() {
        return this.consoleData.locale;
    }

    localeMenuItems() {
        return this.consoleData.localeMenuItems;
    }

    localize(key: string) {
        return JGConsole.localize(this.l10nMessages, this.locale(), key);
    }

    /**
     * Callback (action) from menu.
     */    
    addConlet(type: string) {
        const data = this.conletTypeMgr.types()
            .filter(el => el.type === type)[0];
        this.console.addConlet(data.type, data.renderModes);
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

    get notificationMgr() {
        return this.notificationManager;
    }

    notification(content: string, options: NotificationOptions): Notification {
        const closer = this.notificationManager.notification(content, options);
        return {
            close: closer
        } as Notification;
    }
}
