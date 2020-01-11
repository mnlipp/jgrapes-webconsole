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

import JGPortal from "../portal-base-resource/jgportal.js"
import Vue from "../page-resource/vue/vue.esm.browser.js";
import Vuex from "./lib/vuex.esm.browser.js";

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 * 
 * @module vuejsportal
 */
export const VueJsPortal = {};
export default VueJsPortal;

var log = JGPortal.log;

VueJsPortal.Renderer = class extends JGPortal.Renderer {

    constructor(localeMenuitems, l10nMessages) {
        super();
        this._lastPreviewLayout = [];
        this._lastTabsLayout = [];
        this._lastXtraInfo = {};
        let self = this;
        // Make renderer available
        Vue.prototype.$portalRenderer = this;
        
        // Prepare portal i18n
        this._l10nMessages = l10nMessages;
        window.portalL10n = function(key) { return self.translate(key) };
        
        // Prepare Vuex Store
        let initialLang = document.querySelector("html")
            .getAttribute('lang') || 'en';
        this._vuexStore = new Vuex.Store({
            state: {
                localeMenuitems,
                lang: initialLang,
                portletTypes: [],
            },
            mutations: {
                lang(state, lang) {
                    state.lang = lang;
                    document.querySelector("html").setAttribute('lang', lang);
                    self.portal().setLocale(lang, false);
                },
                addPortletType(state, item) {
                    state.portletTypes.push(item);
                },
            },
        });
    }
    
    init() {
        let self = this;
        // Start Vue
        let store = this._vuexStore;
        new Vue({ el: 'header', store });
        new Vue({ el: '.portalVue', store });

        // Init tabs
        this._portalTabs().addTab({ label: "Overview", 
            id: "portalOverviewPanel", 
            l10n: window.portalL10n });

        // Grid
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            alwaysShowResizeHandle: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        $('#portalPreviews').gridstack(options);
        $('#portalPreviews').on('change', function(event, items) {
            self._layoutChanged();
        });
    }

    _portalTabs() {
        return document.querySelector("#portalTabs").__vue__;
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
        $("#portalPreviews .grid-stack-item").each(function() {
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
            let portletId = item.find(".portlet-preview[data-portlet-id]")
                .attr("data-portlet-id");
            previewLayout.push(portletId);
            xtraInfo[portletId] = [item.attr("data-gs-x"),
            item.attr("data-gs-y"), item.attr("data-gs-width"),
            item.attr("data-gs-height")]
        });

        let tabsLayout = [];
        for (let tab of this._portalTabs().tabs) {
            let panel = $("#" + tab.id);
            let portletId = panel.attr("data-portlet-id");
            if (portletId) {
                tabsLayout.push(portletId);
            }
        }
        this.portal().updateLayout(previewLayout, tabsLayout, xtraInfo);
    };

    addPortletType(portletType, displayNames, renderModes) {
        // Add to menu
        let self = this;
        let label = function() {
            return displayNames[self._vuexStore.state.lang];
        };
        self._vuexStore.commit('addPortletType', [label, portletType, renderModes]);
    }
    
    connectionLost() {
    }

    connectionRestored() {
    }

    connectionSuspended(resume) {
    }

    portalConfigured() {
    }

    lastPortalLayout(previewLayout, tabsLayout, xtraInfo) {
        this._lastPreviewLayout = previewLayout;
        this._lastTabsLayout = tabsLayout;
        this._lastXtraInfo = xtraInfo;
    }

    updatePortletPreview(isNew, container, modes, content, foreground) {
        // Container is:
        //     <section class='portlet portlet-preview' data-portlet-id='...' 
        //     data-portlet-grid-columns='...' data-portlet-grid-rows='   '></section>"
        let self = this;
        container = $(container);
        let newContent = $(content);
        if (isNew) {
            container.append('<header class="ui-draggable-handle"></header>'
                + '<section class="portlet-content"></section>');
            this._setModeIcons(container, modes);

            // Get grid info
            let portletId = container.attr("data-portlet-id");
            let options = {}
            if (portletId in this._lastXtraInfo) {
                options.x = this._lastXtraInfo[portletId][0];
                options.y = this._lastXtraInfo[portletId][1];
                options.width = this._lastXtraInfo[portletId][2];
                options.height = this._lastXtraInfo[portletId][3];
            } else {
                options.autoPosition = true;
                options.width = 4;
                options.height = 4;
                if (newContent.attr("data-portlet-grid-columns")) {
                    options.width = newContent.attr("data-portlet-grid-columns");
                }
                if (newContent.attr("data-portlet-grid-rows")) {
                    options.height = newContent.attr("data-portlet-grid-rows");
                }
                if ($(window).width() < 1200) {
                    let winWidth = Math.max(320, $(window).width());
                    options.width = Math.round(width + (12 - width) * (1 - (winWidth - 320) / (1200 - 320)));
                }
            }

            // Put into grid item wrapper
            let gridItem = $('<div class="grid-stack-item" data-gs-auto-position="1"'
                + ' role="gridcell"></div>');
            container.addClass('grid-stack-item-content');
            gridItem.append(container);

            // Finally add to grid
            let grid = $('#portalPreviews').data('gridstack');
            grid.addWidget(gridItem, options);

            this._layoutChanged();
        }
        let portletHeader = container.children("header");
        portletHeader.find("p").remove();
        portletHeader.prepend("<p>"
            + newContent.attr("data-portlet-title") + "</p>");
        let previewContent = container.children("section");
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            this._portalTabs().selectTab("portalOverviewPanel");
        }
    }

    updatePortletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="portlet portlet-view 
        //              data-portlet-id='...'"></article>"
        let self = this;
        container = $(container);
        let newContent = $(content);
        let portletId = container.attr("data-portlet-id");
        let panelId = "portlet-panel-" + portletId;
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
            for (let tab of this._portalTabs().tabs) {
                if (tab.id === panelId) {
                    tab.label = newContent.attr("data-portlet-title");
                }
            }
        } else {
            container.attr("id", panelId);
            container.attr("hidden", "");
            container.append(newContent);
            let portalPanels = $("#portalPanels");
            portalPanels.append(container);
            // Add to tab list
            this._portalTabs().addTab({ 
                label: newContent.attr("data-portlet-title"), 
                id: panelId, 
                l10n: window.portalL10n,
                removeCallback: function() { self.portal().removeView(portletId); }
            });
            this._layoutChanged();
        }
        if (foreground) {
            this._portalTabs().selectTab(panelId);
        }
    }

    _setModeIcons(portlet, modes) {
        let self = this;
        let portletHeader = portlet.children("header");
        portletHeader.find("button").remove();
        if (modes.includes("Edit")) {
            let button = $("<button type='button' class='fa fa-wrench'></button>");
            button.on("click", function() {
                let button = $(this);
                let portletId = button.closest(".portlet").attr("data-portlet-id");
                self.portal().renderPortlet(portletId, ["Edit", "Foreground"]);
            });
            portletHeader.append(button);
        }
        if (modes.includes("DeleteablePreview")) {
            let button = $("<button type='button' class='fa fa-times'></button>");
            button.on("click", function() {
                let button = $(this);
                let portletId = button.closest(".portlet").attr("data-portlet-id");
                self.portal().removePreview(portletId);
            });
            portletHeader.append(button);
        }
        if (modes.includes("View")) {
            let button = $("<button type='button' class='fa fa-expand'></button>");
            button.on("click", function() {
                let button = $(this);
                let portletId = button.closest(".portlet").attr("data-portlet-id");
                self.portal().renderPortlet(portletId, ["View", "Foreground"]);
            });
            portletHeader.append(button);
        }
    }

    removePortletDisplays(containers) {
        let self = this;
        containers.forEach(function(container) {
            container = $(container);
            if (container.hasClass('portlet-preview')) {
                let grid = $('#portalPreviews').data('gridstack');
                let gridItem = container.closest(".grid-stack-item");
                grid.removeWidget(gridItem);
            }
            if (container.hasClass('portlet-view')) {
                let panelId = container.attr("id");
                self._portalTabs().removeTab(panelId);
                container.remove();
                self._layoutChanged();
            }
        });
        this._layoutChanged();
    }

    notification(content, options) {
    }

    /**
     * Update the title of the portlet with the given id.
     *
     * @param {string} portletId the portlet id
     * @param {string} title the new title
     */
    updatePortletTitle(portletId, title) {
        let preview = this.findPortletPreview(portletId);
        if (preview) {
            preview = $(preview);
            let portletHeader = preview.children("section:first-child > header");
            portletHeader.children("p:first-child").text(title);
        }
        for (let tab of this._portalTabs().tabs) {
            if (tab.id === "portlet-panel-" + portletId) {
                tab.label = title;
            }
        }
    }

    /**
     * Update the modes of the portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     * @param {string[]} modes the modes
     */
    updatePortletModes(portletId, modes) {
        let portlet = this.findPortletPreview(portletId);
        if (!portlet) {
            return;
        }
        this._setModeIcons(portlet, modes);
    }

    showEditDialog(container, modes, content) {
        let self = this;
        let dialog = new (Vue.component('jgp-modal-dialog'))({
            propsData: {
                content: content,
                contentClasses: ["portlet-content"],
                onClose: function(applyChanges) {
                    if (applyChanges) {
                        self.portal().execOnApply(container);
                    }
                    container.parentNode.removeChild(container);
                }
            }
        });
        dialog.$mount();
        let contentRoot = dialog.$el.querySelector(".portlet-content")
            .querySelector("* > [title]");
        if (contentRoot) {
            dialog.title = contentRoot.getAttribute("title");
        }
        $(container).append(dialog.$el);
        dialog.open();
    }
}
