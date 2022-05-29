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

import JGConsole, { RenderMode } from "../console-base-resource/jgconsole.js"

/**
 * @module b4uiconsole
 */
export const B4UIConsole = {};
export default B4UIConsole;

var log = JGConsole.Log;

B4UIConsole.Renderer = class extends JGConsole.Renderer {

    constructor(console, l10n) {
        super(console);
        B4UIConsole.l10n = l10n;
        this._connectionLostNotification = null;
        this._lastPreviewLayout = [];
        this._lastTabsLayout = [];
        this._lastXtraInfo = {};
        this._tabCounter = 1;
        this._conletDisplayNames = {};
    }

    init() {
        let _this = this;
        log.debug("Locking screen");

        $("#consoleLanguageMenu").on("click", "[data-locale]", function() {
            _this.console.setLocale($(this).data("locale"), true);
        });

        // Grid
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            handle: '.card-header',
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

        // Tabs
        var tabs = $("#consoleTabs");

        // Close icon: removing the tab on click
        tabs.on("click", ".conlet-tab-close", function() {
            var tabId = $(this).closest("li").remove().attr("data-conlet-tab");
            $("#" + tabId + "-pane").remove();
            $("#consoleOverviewTab").tab('show');
            _this._layoutChanged();
        });

    }

    connectionLost() {
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                this.notification(B4UIConsole.l10n.serverConnectionLost, {
                    error: true,
                    closeable: false,
                });
        }
    }

    connectionRestored() {
        if (this._connectionLostNotification != null) {
            this._connectionLostNotification.alert("close");
        }
        this.notification(B4UIConsole.l10n.serverConnectionRestored, {
            type: "success",
            autoClose: 2000,
        });
    }

    connectionSuspended(resume) {
        let dialog = $('<div class="modal" tabindex="-1" role="dialog">'
            + '<div class="modal-dialog" role="document">'
            + '<div class="modal-content">'
            + '<div class="modal-header">'
            + '<h5 class="modal-title">' 
            + B4UIConsole.l10n.sessionSuspendedTitle + '</h5>'
            + '</button></div>'
            + '<div class="modal-body">'
            + '<p>' + B4UIConsole.l10n.sessionSuspendedMessage + '</p>'
            + '</div>'
            + '<div class="modal-footer">'
            + '<button type="button" class="btn btn-primary" data-dismiss="modal">'
            + B4UIConsole.l10n.resume + '</button>'
            + '</div>'
            + '</div>'
            + '</div>'
            + '</div>');
        dialog.find('.modal-footer button').on('click', function() {
            resume();
        });
        dialog.modal();
    }

    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("#loader-overlay").fadeOut("slow");
    }

    addConletType(conletType, displayNames, renderModes) {
        if (!renderModes.includes(RenderMode.Preview)
            && !renderModes.includes(RenderMode.View)) {
            return;
        }
        // Add to menu
        this._conletDisplayNames[conletType] = displayNames;
        let _this = this;
        let lang = document.querySelector("html").getAttribute('lang');
        let displayName = JGConsole.forLang(displayNames, lang);
        let item = $('<a class="dropdown-item" href="#" data-conlet-type="'
            + conletType + '">' + displayName + '</a>');
        item.data("render-modes", renderModes)
        item.on('click', function(e) {
            _this.console.addConlet($(this).data("conlet-type"),
                $(this).data("render-modes"));
        });
        let inserted = false;
        $("#consoleNavbarConletList").find(".dropdown-item").each(function(index, el) {
            if (displayName < $(el).text()) {
                $(el).before(item);
                inserted = true;
                return false;
            }
        });
        if (!inserted) {
            $("#consoleNavbarConletList").append(item);
        }
    }

    removeConletType(conletType) {
        // Remove from menu
        let _this = this;
        $("#consoleNavbarConletList").find(".dropdown-item").each(function(index, el) {
            if (conletType == $(el).data("conletType")) {
                $(el).remove();
                return false;
            }
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
            container.addClass('card');
            container.append('<h6 class="card-header conlet-preview-header '
                + 'd-flex flex-row"></h6>'
                + '<div class="card-body conlet-content conlet-preview-content">'
                + '</div>');
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
            _this._previewGrid.addWidget(gridItem, options);

            this._layoutChanged();
        }
        let conletHeader = container.find(".conlet-preview-header");
        conletHeader.find(".conlet-preview-title").remove();
        conletHeader.prepend("<span class='conlet-preview-title flex-fill'>"
            + this._evaluateTitle(container, newContent) + "</span>");
        let previewContent = container.find(".conlet-preview-content");
        this._styleSemantics(newContent);
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            $("#consoleOverviewTab").tab('show');
        }
    }

    _setModeIcons(conlet, modes) {
        let _this = this;
        let conletHeader = conlet.find(".conlet-preview-header");
        conletHeader.find(".conlet-preview-icon").remove();
        if (modes.includes(RenderMode.Edit)) {
            conletHeader.append("<a href='#' class='" +
                "conlet-preview-icon conlet-edit" +
                " ml-2 fa fa-wrench' role='button'></a>");
            conletHeader.find(".conlet-edit").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                _this.console.renderConlet(conletId, [RenderMode.Edit, RenderMode.Foreground]);
            });
        }
        if (!modes.includes(RenderMode.StickyPreview)) {
            conletHeader.append("<a href='#' class='" +
                "conlet-preview-icon conlet-delete" +
                " ml-2 fa fa-times' role='button'></a>");
            conletHeader.find(".conlet-delete").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                _this.console.removePreview(conletId);
            });
        }
        if (modes.includes("View")) {
            conletHeader.append("<a href='#' class='" +
                "conlet-preview-icon conlet-expand" +
                " ml-2 fa fa-expand' role='button'></a>");
            conletHeader.find(".conlet-expand").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                let conletView = _this.findConletView(conletId);
                if (conletView) {
                    _this._activateConletView($(conletView));
                } else {
                    _this.console.renderConlet(conletId, ["View", "Foreground"]);
                }
            });
        }
    }

    _activateConletView(conlet) {
        let tabId = conlet.attr("data-conlet-tab-tab");
        $("#" + tabId).tab('show');
    }

    updateConletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        container = $(container);
        let newContent = $(content);
        this._styleSemantics(newContent);
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
        } else {
            this._tabCounter += 1;
            let id = "conlet-tab-" + this._tabCounter;
            let tabs = $("#consoleTabs");
            let tab = $('<li class="nav-item" data-conlet-tab="' + id + '"'
                + ' data-conlet-id="' + container.attr("data-conlet-id") + '">'
                + '<a class="nav-link" role="tab"'
                + ' id="' + id + '-tab" data-toggle="tab"'
                + ' href="#' + id + '-pane"'
                + ' alt="' + B4UIConsole.l10n.close + '"'
                + '>' + this._evaluateTitle(container, newContent)
                + '<span class="fa fa-times ml-2 text-primary conlet-tab-close"></span>'
                + '</a>'
                + '</li>');
            tabs.append(tab);

            container.addClass("tab-pane fade m-3");
            container.attr("id", id + "-pane");
            container.attr("data-conlet-tab-tab", id + "-tab");
            container.attr("role", "tabpanel");
            container.attr("aria-labelledby", id + "-tab");
            container.append(newContent);
            let tabPanes = $("#consoleTabPanes");
            // JQuery append seems to have a delay.
            tabPanes[0].appendChild(container[0]);
            this._layoutChanged();
        }
        if (foreground) {
            this._activateConletView(container);
        }
    }

    _evaluateTitle(container, content) {
        let title = content.attr("data-conlet-title");
        if (!title) {
            let conletType = container.attr("data-conlet-type");
            let lang = document.querySelector("html").getAttribute('lang');
            title = JGConsole.forLang(this._conletDisplayNames[conletType], lang);
        }
        return title;
    }

    _styleSemantics(content) {
        /*
        let toBeStyled = content.find("[data-style-semantics]")
            .addBack("[data-style-semantics]");
        toBeStyled.each(function() {
           let subtree = $(this);
           subtree.find("*").addBack().each(function() {
               let node = $(this);
               let tagName = node.prop("tagName");
               if (tagName == "BUTTON" && !node.hasClass("btn")) {
                   node.addClass("btn btn-primary")
               }
           })
        });
        */
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
                $("#consoleOverviewTab").tab('show');
                var tabId = container.attr("data-conlet-tab-tab");
                $("#" + tabId).closest("li").remove();
                container.remove();
            }
        });
        this._layoutChanged();
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
        $("#consoleTabPanes > [data-conlet-id]").each(function(index) {
            let conletId = $(this).attr("data-conlet-id");
            tabsLayout.push(conletId);
        });

        this.console.updateLayout(previewLayout, tabsLayout, xtraInfo);
    }

    /**
     * Update the title of the conlet with the given id.
     *
     * @param {string} conletId the conlet id
     * @param {string} title the new title
     */
    updateConletTitle(conletId, title) {
        let preview = $("#consolePreviews .conlet-preview[data-conlet-id='"
            + conletId + "'");
        if (preview.length > 0) {
            let conletHeader = preview.find(".conlet-preview-header");
            conletHeader.find(".conlet-preview-title").remove();
            conletHeader.prepend("<span class='conlet-preview-title flex-fill'>"
                + title + "</span>");
        }
        let view = $("#consoleTabs .nav-item[data-conlet-id='" + conletId + "']");
        if (view.length > 0) {
            let titleNode = view.find(".nav-link");
            let close = titleNode.find(".conlet-tab-close").detach();
            titleNode.empty();
            titleNode.append(title);
            titleNode.append(close);
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

    openModalDialog(container, options, content) {
        let _this = this;
        container = $(container);
        let dialog = $('<div class="modal" tabindex="-1" role="dialog">'
            + '<div class="modal-dialog modal-lg" role="document">'
            + '<div class="modal-content">'
            + '<div class="modal-header">'
            + '<button type="button" class="close" data-dismiss="modal"'
            + ' alt="' + B4UIConsole.l10n.close + '">'
            + '<span aria-hidden="true">&times;</span>'
            + '</button></div>'
            + '<div class="modal-body">'
            + '</div>'
            + '<div class="modal-footer">'
            + '<button type="button" class="btn btn-primary" data-dismiss="modal">'
            + B4UIConsole.l10n.ok + '</button>'
            + '</div>'
            + '</div>'
            + '</div>'
            + '</div>');
        dialog.find(".modal-body").append($(content));
        dialog.find(".btn-primary").on('click', function() {
            _this.console.execOnAction(container[0], true, true);
        });
        container.append(dialog);
        dialog.modal();
    }
}
