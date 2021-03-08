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

'use strict';

import { JGConsole, RenderMode } from "../console-base-resource/jgconsole.js"
import { reactive, ref, createApp, onMounted, computed }
    from "../page-resource/vue/vue.esm-browser.js";
// import { AashPlugin } from "../page-resource/jgwc-vue-components/jgwc-components.js";
import AashPlugin, { provideApi, getApi, AashModalDialog } 
    from "../page-resource/aash-vue-components/lib/aash-vue-components.js";

/**
 * JGConsole establishes a namespace for the JavaScript functions
 * that are provided by the console.
 * 
 * @module vuejsconsole
 */
export const VueJsConsole = {};
export default VueJsConsole;

var log = JGConsole.Log;

VueJsConsole.Renderer = class extends JGConsole.Renderer {

    constructor(localeMenuitems, l10nMessages) {
        super();
        this._lastPreviewLayout = [];
        this._lastTabsLayout = [];
        this._lastXtraInfo = {};
        this._connectionLostNotification = null;
        let _this = this;
        
        // Prepare console i18n
        this._l10nMessages = l10nMessages;
        window.consoleL10n = function(key) { return _this.localize(key) };
        
        // Shared state
        this._localeMenuItems = reactive(localeMenuitems);
        this._lang = ref(document.querySelector("html")
            .getAttribute('lang') || 'en');
        this._conletTypes = reactive([]);
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
            label: function() { return _this.localize("Overview") }, 
            l10n: window.consoleL10n });

        // Grid
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            alwaysShowResizeHandle: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        _this._previewGrid = GridStack.init(options, '#consolePreviews');
        if (!_this._previewGrid) {
            log.error("VueJsConsole: Creating preview grid failed.")
        }
        $('#consolePreviews').on('change', function(event, items) {
            _this._layoutChanged();
        });
    }

    locale() {
        return this._lang.value;
    }

    setLocale(lang) {
        this._lang.value = lang;
        document.querySelector("html").setAttribute('lang', lang);
        this.console().setLocale(lang, false);
        
    }

    localeMenuItems() {
        return this._localeMenuItems;
    }

    conletTypes() {
        return this._conletTypes;
    }

    _consoleTabs() {
        return getApi(document.querySelector("#consoleTabs"));
    }

    localize(key) {
        return JGConsole.localize(this._l10nMessages, 
            this.locale(), key);
    }
    
    _layoutChanged() {
        let gridItems = [];
        $("#consolePreviews .grid-stack-item").each(function() {
            gridItems.push($(this));
        });
        gridItems.sort(function(a, b) {
            if (a.attr("data-gs-y") != b.attr("data-gs-y")) {
                return b.attr("data-gs-y") - a.attr("data-gs-y");
            }
            return b.attr("data-gs-x") - a.attr("data-gs-x");
        });

        let previewLayout = [];
        let xtraInfo = {};
        gridItems.forEach(function(item) {
            let conletId = item.find(".conlet-preview[data-conlet-id]")
                .attr("data-conlet-id");
            previewLayout.push(conletId);
            xtraInfo[conletId] = [item.attr("data-gs-x"),
            item.attr("data-gs-y"), item.attr("data-gs-width"),
            item.attr("data-gs-height")]
        });

        let tabsLayout = [];
        for (let panel of this._consoleTabs().panels()) {
            let tabpanel = $("[id='" + panel.id + "']");
            let conletId = tabpanel.attr("data-conlet-id");
            if (conletId) {
                tabsLayout.push(conletId);
            }
        }
        this.console().updateLayout(previewLayout, tabsLayout, xtraInfo);
    };

    addConletType(conletType, displayNames, renderModes) {
        // Add to menu
        let _this = this;
        let label = function() {
            return JGConsole.forLang(displayNames, _this.locale()) || "Conlet";
        };
        _this._conletTypes.push([label, conletType, renderModes]);
    }
    
    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("#loader-overlay").fadeOut("100");
    }

    connectionSuspended(resume) {
        let _this = this;
        let dialog = createApp(AashModalDialog, {
            title: _this.localize("Console Session Suspended"),
            showCancel: false,
            content: _this.localize("consoleSessionSuspendedMessage"),
            contentClasses: [],
            closeLabel: _this.localize("Resume"),
            onClose: function(applyChanges) {
                dialog.unmount();
                resume();
            }
        });
        let node = dialog.mount(document.querySelector("#modal-dialog-slot"))
            .$el.firstChild;
        getApi(node).open();
    }

    connectionLost() {
        let _this = this;
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                this.notification(
                    _this.localize("serverConnectionLostMessage"), {
                    error: true,
                    closeable: false,
                });
        }
    }

    connectionRestored() {
        if (this._connectionLostNotification != null) {
            this._connectionLostNotification.close();
        }
        this.notification(this.localize("serverConnectionRestoredMessage"), {
            type: "success",
            autoClose: 2000,
        });
    }

    lastConsoleLayout(previewLayout, tabsLayout, xtraInfo) {
        this._lastPreviewLayout = previewLayout;
        this._lastTabsLayout = tabsLayout;
        this._lastXtraInfo = xtraInfo;
    }

    updateConletPreview(isNew, container, modes, content, foreground) {
        // Container is:
        //     <section class='conlet conlet-preview' data-conlet-id='...' 
        //     data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>"
        let _this = this;
        container = $(container);
        let newContent = $(content);
        if (isNew) {
            container.append('<header class="ui-draggable-handle"></header>'
                + '<section class="conlet-content"></section>');

            // Get grid info
            let conletId = container.attr("data-conlet-id");
            let options = {}
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
                if (newContent.attr("data-conlet-grid-columns")) {
                    options.width = newContent.attr("data-conlet-grid-columns");
                }
                if (newContent.attr("data-conlet-grid-rows")) {
                    options.height = newContent.attr("data-conlet-grid-rows");
                }
                if ($(window).width() < 1200) {
                    let winWidth = Math.max(320, $(window).width());
                    let width = options.width;
                    width = Math.round(width + (12 - width) 
                        * (1 - (winWidth - 320) / (1200 - 320)));
                    options.width = width;
                }
            }

            // Put into grid item wrapper
            let gridItem = $('<div class="grid-stack-item" data-gs-auto-position="1"'
                + ' role="gridcell"></div>');
            container.addClass('grid-stack-item-content');
            gridItem.append(container);

            // Finally add to grid
            this._previewGrid.addWidget(gridItem, options);
            this._layoutChanged();

            // Generate header            
            this._mountHeader($(container).children("header")[0], conletId);
        }
        let headerComponent = getApi(container.children("header")[0]);
        headerComponent.setTitle(_this._evaluateTitle(container, newContent));
        headerComponent.setModes(modes);
        let previewContent = container.children("section");
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            this._consoleTabs().selectPanel("consoleOverviewPanel");
        }
    }

    _mountHeader(header, conletId) {
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
                const modes = reactive([]);

                const setTitle = (value) => {
                    title.value = value;
                }
                
                const evalTitle = computed(() => {
                    if (typeof title.value === 'function') {
                        return title.value();
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
                    _this.console().renderConlet(
                        conletId, [RenderMode.Edit, RenderMode.Foreground]);
                };
                
                const removePreview = () => {
                    _this.console().removePreview(conletId)
                };
                
                const showView = () => {
                    _this.console().renderConlet(
                        conletId, ["View", "Foreground"]);
                };

                provideApi (header, {
                    setTitle, isEditable, isRemovable,
                    hasView, edit, removePreview, showView,
                    setModes: (newModes) => { 
                        modes.length = 0;
                        modes.push(...newModes);
                    }
                });

                return { conletId, evalTitle, isEditable, isRemovable,
                    hasView, edit, removePreview, showView, header }
            }
        }).mount(header);
    }

    updateConletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        let _this = this;
        container = $(container);
        let newContent = $(content);
        let conletId = container.attr("data-conlet-id");
        let panelId = "conlet-panel-" + conletId;
        if (isNew) {
            container.attr("id", panelId);
            container.attr("hidden", "");
            container.append(newContent);
            let consolePanels = $("#consolePanels");
            // JQuery append seems to have a delay.
            consolePanels[0].appendChild(container[0]);
            // Add to tab list
            this._consoleTabs().addPanel({
                id: panelId, 
                label: _this._evaluateTitle(container, newContent), 
                l10n: window.consoleL10n,
                removeCallback: function() { _this.console().removeView(conletId); }
            });
            this._layoutChanged();
        } else {
            container.children().detach();
            container.append(newContent);
            for (let panel of this._consoleTabs().panels()) {
                if (panel.id === panelId) {
                    panel.label = _this._evaluateTitle(container, newContent);
                }
            }
        }
        if (foreground) {
            this._consoleTabs().selectPanel(panelId);
        }
    }

    _evaluateTitle(container, content) {
        let title = content.attr("data-conlet-title");
        if (!title) {
            let conletType = container.attr("data-conlet-type");
            for (let item of this.conletTypes()) {
                if (item[1] === conletType) {
                    title = item[0];
                    break;
                }
            }
        }
        return title;
    }
    
    removeConletDisplays(containers) {
        let _this = this;
        containers.forEach(function(container) {
            container = $(container);
            if (container.hasClass('conlet-preview')) {
                let gridItem = container.closest(".grid-stack-item");
                _this._previewGrid.removeWidget(gridItem);
            }
            if (container.hasClass('conlet-view')) {
                let panelId = container.attr("id");
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
     * @param {string} conletId the conlet id
     * @param {string} title the new title
     */
    updateConletTitle(conletId, title) {
        let preview = this.findConletPreview(conletId);
        if (preview) {
            let conletHeader = $(preview).children("section:first-child > header")[0];
            let headerComponent = getApi(conletHeader);
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
     * @param {string} conletId the conlet id
     * @param {string[]} modes the modes
     */
    updateConletModes(conletId, modes) {
        let conlet = this.findConletPreview(conletId);
        if (!conlet) {
            return;
        }
        let headerComponent = getApi($(conlet).children("header")[0]);
        headerComponent.setModes(modes);
    }

    showEditDialog(container, modes, content) {
        let _this = this;
        let dialog = createApp (AashModalDialog, {
            content: content,
            contentClasses: ["conlet-content"],
            onClose: function(applyChanges) {
                if (applyChanges) {
                    _this.console().execOnApply(container);
                }
                dialog.unmount();
            }
        });
        
        let dialogEl = dialog.mount(container).$el;
        let dialogApi = getApi(dialogEl.firstChild);
        let contentRoot = dialogEl.querySelector(".conlet-content")
            .querySelector("* > [title]");
        if (contentRoot) {
            dialogApi.updateTitle(contentRoot.getAttribute("title"));
        }
        dialogApi.open();
    }
    
    notification(content, options) {
        let notificationArea = document.querySelector("#notification-area");
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
                const error = ('error' in options) ? options.error : false;
                const type = ('type' in options) ? options.type : "info";
                const closeable = ('closeable' in options) ? options.closeable : true;
                const autoClose = ('autoClose' in options) ? options.autoClose : false;
                const notificationClass = (() => {
                    if (error || type == "error") {
                        return "notification--error"
                    }
                    if (type == "success") {
                        return "notification--success";
                    }
                    if (type == "warning") {
                        return "notification--warning";
                    }
                    if (type == "danger") {
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
        return getApi(notification.mount(notificationNode).$el);
    }

}
