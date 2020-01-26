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

import JGConsole from "../console-base-resource/jgconsole.js"
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
        window.consoleL10n = function(key) { return _this.translate(key) };
        
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
        this._consoleTabs().addTab({ label: "Overview", 
            id: "consoleOverviewPanel", 
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

    translate(key) {
        let lang = this._vuexStore.state.lang;
        while (true) {
            let langMsgs = this._l10nMessages[lang];
            if (langMsgs) {
                let result = langMsgs[key];
                if (result) {
                    return result;
                }
            }
            if (lang === 'en') {
                return key
            }
            lang = 'en';
        }
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
        for (let tab of this._consoleTabs().tabs) {
            let panel = $("[id='" + tab.id + "']");
            let conletId = panel.attr("data-conlet-id");
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
                title: _this.translate("Console Session Suspended"),
                showCancel: false,
                content: _this.translate("consoleSessionSuspendedMessage"),
                contentClasses: [],
                closeLabel: _this.translate("Resume"),
                onClose: function(applyChanges) {
                    document.querySelector("body").removeChild(dialog.$el);
                    resume();
                }
            }
        });
        dialog.$mount();
        $("body").append(dialog.$el);
        dialog.open();
    }

    connectionRestored() {
    }

    connectionLost() {
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                this.notification(
                    JGConsole.forLang(this._l10nMessages, 
                        _this._vuexStore.state.lang).serverConnectionLost, {
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
            this._setModeIcons(container, modes);

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
        }
        let conletHeader = container.children("header");
        conletHeader.find("p").remove();
        conletHeader.prepend("<p>"
            + newContent.attr("data-conlet-title") + "</p>");
        let previewContent = container.children("section");
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            this._consoleTabs().selectTab("consoleOverviewPanel");
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
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
            for (let tab of this._consoleTabs().tabs) {
                if (tab.id === panelId) {
                    tab.label = newContent.attr("data-conlet-title");
                }
            }
        } else {
            container.attr("id", panelId);
            container.attr("hidden", "");
            container.append(newContent);
            let consolePanels = $("#consolePanels");
            // JQuery append seems to have a delay.
            consolePanels[0].appendChild(container[0]);
            // Add to tab list
            this._consoleTabs().addTab({ 
                label: newContent.attr("data-conlet-title"), 
                id: panelId, 
                l10n: window.consoleL10n,
                removeCallback: function() { _this.console().removeView(conletId); }
            });
            this._layoutChanged();
        }
        if (foreground) {
            this._consoleTabs().selectTab(panelId);
        }
    }

    _setModeIcons(conlet, modes) {
        let _this = this;
        let conletHeader = conlet.children("header");
        conletHeader.find("button").remove();
        if (modes.includes("Edit")) {
            let button = $("<button type='button' class='fa fa-wrench'></button>");
            button.on("click", function() {
                let button = $(this);
                let conletId = button.closest(".conlet").attr("data-conlet-id");
                _this.console().renderConlet(conletId, ["Edit", "Foreground"]);
            });
            conletHeader.append(button);
        }
        if (modes.includes("DeleteablePreview")) {
            let button = $("<button type='button' class='fa fa-times'></button>");
            button.on("click", function() {
                let button = $(this);
                let conletId = button.closest(".conlet").attr("data-conlet-id");
                _this.console().removePreview(conletId);
            });
            conletHeader.append(button);
        }
        if (modes.includes("View")) {
            let button = $("<button type='button' class='fa fa-expand'></button>");
            button.on("click", function() {
                let button = $(this);
                let conletId = button.closest(".conlet").attr("data-conlet-id");
                _this.console().renderConlet(conletId, ["View", "Foreground"]);
            });
            conletHeader.append(button);
        }
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
                _this._consoleTabs().removeTab(panelId);
                container.remove();
                _this._layoutChanged();
            }
        });
        this._consoleTabs().selectTab("consoleOverviewPanel");
        this._layoutChanged();
    }

    notification(content, options) {
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
        for (let tab of this._consoleTabs().tabs) {
            if (tab.id === "conlet-panel-" + conletId) {
                tab.label = title;
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
        this._setModeIcons($(conlet), modes);
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
                    container.parentNode.removeChild(container);
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
        let notification = $('<div class="alert alert-dismissible fade show"'
            + ' role="alert"></div>');
        if (('error' in options) && options.error) {
            notification.addClass("alert-danger");
        } else if ('type' in options) {
            if (options.type == 'success') {
                notification.addClass("alert-success");
            } else if (options.type == 'success') {
                notification.addClass("alert-success");
            } else if (options.type == 'warning') {
                notification.addClass("alert-warning");
            } else if (options.type == 'error') {
                notification.addClass("alert-danger");
            } else {
                notification.addClass("alert-info");
            }
        } else {
            notification.addClass("alert-info");
        }
        let parsed = $("<div>" + content + "</div>");
        if (parsed.text() == "" && parsed.children.size() > 0) {
            parsed = parsed.children();
        }
        notification.append(parsed);
        if (!('closeable' in options) || options.closeable) {
            notification.append($('<button type="button" class="close"'
                + ' data-dismiss="alert" aria-label="Close">'
                + '<span aria-hidden="true">&times;</span></button>'));
        }
        $("#notification-area").prepend(notification);
        if ('autoClose' in options) {
            setTimeout(function() {
                notification.alert('close');
            }, options.autoClose);
        }
        return notification;
    }

}
