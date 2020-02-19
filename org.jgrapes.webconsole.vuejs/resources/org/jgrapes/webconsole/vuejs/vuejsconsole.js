/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2019  Michael N. Lipp
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
import Vue from "../page-resource/vue/vue.esm.browser.js";
import Vuex from "../page-resource/vuex/vuex.esm.browser.js";

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
        // Make renderer available
        Vue.prototype.$consoleRenderer = this;
        
        // Prepare console i18n
        this._l10nMessages = l10nMessages;
        window.consoleL10n = function(key) { return _this.localize(key) };
        
        // Prepare Vuex Store
        let initialLang = document.querySelector("html")
            .getAttribute('lang') || 'en';
        this._vuexStore = new Vuex.Store({
            state: {
                localeMenuitems,
                lang: initialLang,
                conletTypes: [],
            },
            mutations: {
                lang(state, lang) {
                    state.lang = lang;
                    document.querySelector("html").setAttribute('lang', lang);
                    _this.console().setLocale(lang, false);
                },
                addConletType(state, item) {
                    state.conletTypes.push(item);
                },
            },
        });
    }
    
    init() {
        let _this = this;
        log.debug("Locking screen");
        
        // Start Vue
        let store = this._vuexStore;
        new Vue({ el: 'header', store });
        new Vue({ el: '.consoleVue', store });

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
        $('#consolePreviews').gridstack(options);
        _this._previewGrid = $('#consolePreviews').data('gridstack');
        if (!_this._previewGrid) {
            log.error("VueJsConsole: Creating preview grid failed.")
        }
        $('#consolePreviews').on('change', function(event, items) {
            _this._layoutChanged();
        });
    }

    _consoleTabs() {
        return document.querySelector("#consoleTabs").__vue__;
    }

    localize(key) {
        return JGConsole.localize(this._l10nMessages, 
            this._vuexStore.state.lang, key);
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
        for (let panel of this._consoleTabs().panels) {
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
            return JGConsole.forLang(displayNames, _this._vuexStore.state.lang) || "Conlet";
        };
        _this._vuexStore.commit('addConletType', [label, conletType, renderModes]);
    }
    
    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("#loader-overlay").fadeOut("100");
    }

    connectionSuspended(resume) {
        let _this = this;
        let dialog = new (Vue.component('jgwc-modal-dialog'))({
            propsData: {
                title: _this.localize("Console Session Suspended"),
                showCancel: false,
                content: _this.localize("consoleSessionSuspendedMessage"),
                contentClasses: [],
                closeLabel: _this.localize("Resume"),
                onClose: function(applyChanges) {
                    this.$destroy();
                    this.$el.remove();
                    resume();
                }
            }
        });
        dialog.$mount();
        $("body").append(dialog.$el);
        dialog.open();
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
            
            new Vue({
                el: $(container).children("header")[0],
                data: {
                    conletId: conletId,
                    title: "",
                    modes: [],
                },
                computed: {
                    evalTitle: function() {
                        if (typeof this.title === 'function') {
                            return this.title();
                        }
                        return this.title;
                    },
                    isEditable: function() {
                        return modes.includes(RenderMode.Edit);
                    },
                    isRemovable: function() {
                        return !this.modes.includes(RenderMode.StickyPreview);
                    },
                    hasView: function() {
                        return this.modes.includes(RenderMode.View);
                    },
                },
                methods: {
                    edit: function() {
                        _this.console().renderConlet(
                            conletId, [RenderMode.Edit, RenderMode.Foreground]);
                    },
                    removePreview: function() {
                        _this.console().removePreview(conletId)
                    },
                    showView: function() {
                        _this.console().renderConlet(
                            conletId, ["View", "Foreground"]);
                    },
                },
                template: `
                  <header class="ui-draggable-handle">
                    <p>{{ evalTitle }}</p>
                    <button v-if="isEditable"
                      type='button' class='fa fa-wrench' @click="edit()"
                    ></button><button v-if="isRemovable" 
                      type="button" class="fa fa-times" @click="removePreview()"
                    ></button><button v-if="hasView"
                      type="button" class="fa fa-expand" @click="showView()"
                    ></button>
                  </header>`
            });
        }
        let vm = container.children("header")[0].__vue__;
        vm.title = _this._evaluateTitle(container, newContent);
        vm.modes = modes;
        let previewContent = container.children("section");
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            this._consoleTabs().selectPanel("consoleOverviewPanel");
        }
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
            for (let panel of this._consoleTabs().panels) {
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
            for (let item of this._vuexStore.state.conletTypes) {
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
                let vm = container.children("header")[0].__vue__;
                vm.$destroy();
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
            let conletHeader = $(preview).children("section:first-child > header");
            conletHeader.children("p:first-child").text(title);
        }
        for (let panel of this._consoleTabs().panels) {
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
        let vm = $(conlet).children("header")[0].__vue__;
        vm.modes = modes;
    }

    showEditDialog(container, modes, content) {
        let _this = this;
        let dialog = new (Vue.component('jgwc-modal-dialog'))({
            propsData: {
                content: content,
                contentClasses: ["conlet-content"],
                onClose: function(applyChanges) {
                    if (applyChanges) {
                        _this.console().execOnApply(container);
                    }
                    this.$destroy();
                    container.remove();
                }
            }
        });
        dialog.$mount();
        let contentRoot = dialog.$el.querySelector(".conlet-content")
            .querySelector("* > [title]");
        if (contentRoot) {
            dialog.title = contentRoot.getAttribute("title");
        }
        $(container).append(dialog.$el);
        dialog.open();
    }
    
    notification(content, options) {
        let notification = new Vue({
            data: {
                message: content,
                error: ('error' in options) ? options.error : false,
                type: ('type' in options) ? options.type : "info",
                closeable: ('closeable' in options) ? options.closeable : true,
                autoClose: ('autoClose' in options) ? options.autoClose : false,
            },
            computed: {
                notificationClass: function() {
                    if (this.error || this.type == "error") {
                        return "notification--error"
                    }
                    if (this.type == "success") {
                        return "notification--success";
                    }
                    if (this.type == "warning") {
                        return "notification--warning";
                    }
                    if (this.type == "danger") {
                        return "notification--danger";
                    }
                    return "notification--info";
                }
            },
            methods: {
                close: function() {
                    this.$destroy();
                    if (this.$el && this.$el.parentNode) {
                        this.$el.remove();
                    }
                }
            },
            mounted: function() {
                let _this = this;
                if (this.autoClose) {
                    setTimeout(function() {
                            _this.close();
                        }, this.autoClose);
                }
            },
            template: `
                <div role="alert" :class="notificationClass">
                  <p v-html="message"></p>
                  <button v-if="closeable" type="button" class="fa fa-times"
                    v-on:click="close()"></button>
                </div>
            `,
        });
        notification.$mount();
        $("#notification-area").append(notification.$el);
        return notification;
    }

}
