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

/**
 * @module b4uiportal
 */
export const B4UIPortal = {};
export default B4UIPortal;

var log = JGPortal.log;

B4UIPortal.Renderer = class extends JGPortal.Renderer {

    constructor(l10n) {
        super();
        B4UIPortal.l10n = l10n;
        this._connectionLostNotification = null;
        this._lastPreviewLayout = [];
        this._lastTabsLayout = [];
        this._lastXtraInfo = {};
        this._tabCounter = 1;
    }

    init() {
        let self = this;
        log.debug("Locking screen");

        $("#portalLanguageMenu").on("click", "[data-locale]", function() {
            self.sendSetLocale($(this).data("locale"), true);
        });

        // Grid
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            handle: '.card-header',
            alwaysShowResizeHandle: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        $('#portalPreviews').gridstack(options);
        $('#portalPreviews').on('change', function(event, items) {
            self._layoutChanged();
        });

        // Tabs
        var tabs = $("#portalTabs");

        // Close icon: removing the tab on click
        tabs.on("click", ".portlet-tab-close", function() {
            var tabId = $(this).closest("li").remove().attr("data-portlet-tab");
            $("#" + tabId + "-pane").remove();
            $("#portalOverviewTab").tab('show');
            self._layoutChanged();
        });

    }

    connectionLost() {
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                this.notification(B4UIPortal.l10n.serverConnectionLost, {
                    error: true,
                    closeable: false,
                });
        }
    }

    connectionRestored() {
        if (this._connectionLostNotification != null) {
            this._connectionLostNotification.alert("close");
        }
        this.notification(B4UIPortal.l10n.serverConnectionRestored, {
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
            + B4UIPortal.l10n.sessionSuspendedTitle + '</h5>'
            + '</button></div>'
            + '<div class="modal-body">'
            + '<p>' + B4UIPortal.l10n.sessionSuspendedMessage + '</p>'
            + '</div>'
            + '<div class="modal-footer">'
            + '<button type="button" class="btn btn-primary" data-dismiss="modal">'
            + B4UIPortal.l10n.ok + '</button>'
            + '</div>'
            + '</div>'
            + '</div>'
            + '</div>');
        dialog.find('.modal-footer button').on('click', function() {
            resume();
        });
        dialog.modal();
    }

    portalConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("#loader-overlay").fadeOut("slow");
    }

    addPortletType(portletType, displayNames, renderModes) {
        // Add to menu
        let self = this;
        let lang = document.querySelector("html").getAttribute('lang');
        let displayName = displayNames[lang];
        let item = $('<a class="dropdown-item" href="#" data-portlet-type="'
            + portletType + '">' + displayName + '</a>');
        item.data("render-modes", renderModes)
        item.on('click', function(e) {
            self.sendAddPortlet($(this).data("portlet-type"),
                $(this).data("render-modes"));
        });
        let inserted = false;
        $("#portalNavbarPortletList").find(".dropdown-item").each(function(index, el) {
            if (displayName < $(el).text()) {
                $(el).before(item);
                inserted = true;
                return false;
            }
        });
        if (!inserted) {
            $("#portalNavbarPortletList").append(item);
        }
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
            container.addClass('card');
            container.append('<h6 class="card-header portlet-preview-header '
                + 'd-flex flex-row"></h6>'
                + '<div class="card-body portlet-content portlet-preview-content">'
                + '</div>');
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
        let portletHeader = container.find(".portlet-preview-header");
        portletHeader.find(".portlet-preview-title").remove();
        portletHeader.prepend("<span class='portlet-preview-title flex-fill'>"
            + newContent.attr("data-portlet-title") + "</span>");
        let previewContent = container.find(".portlet-preview-content");
        this._styleSemantics(newContent);
        previewContent.empty();
        previewContent.append(newContent);
        if (foreground) {
            $("#portalOverviewTab").tab('show');
        }
    }

    _setModeIcons(portlet, modes) {
        let self = this;
        let portletHeader = portlet.find(".portlet-preview-header");
        portletHeader.find(".portlet-preview-icon").remove();
        if (modes.includes("Edit")) {
            portletHeader.append("<a href='#' class='" +
                "portlet-preview-icon portlet-edit" +
                " ml-2 fa fa-wrench' role='button'></a>");
            portletHeader.find(".portlet-edit").on("click", function() {
                let icon = $(this);
                let portletId = icon.closest(".portlet").attr("data-portlet-id");
                self.sendRenderPortlet(portletId, ["Edit", "Foreground"]);
            });
        }
        if (modes.includes("DeleteablePreview")) {
            portletHeader.append("<a href='#' class='" +
                "portlet-preview-icon portlet-delete" +
                " ml-2 fa fa-times' role='button'></a>");
            portletHeader.find(".portlet-delete").on("click", function() {
                let icon = $(this);
                let portletId = icon.closest(".portlet").attr("data-portlet-id");
                self.sendDeletePortlet(portletId);
            });
        }
        if (modes.includes("View")) {
            portletHeader.append("<a href='#' class='" +
                "portlet-preview-icon portlet-expand" +
                " ml-2 fa fa-expand' role='button'></a>");
            portletHeader.find(".portlet-expand").on("click", function() {
                let icon = $(this);
                let portletId = icon.closest(".portlet").attr("data-portlet-id");
                let portletView = self.findPortletView(portletId);
                if (portletView) {
                    self._activatePortletView(portletView);
                } else {
                    self.sendRenderPortlet(portletId, ["View", "Foreground"]);
                }
            });
        }
    }

    _activatePortletView(portlet) {
        let tabId = portlet.attr("data-portlet-tab-tab");
        $("#" + tabId).tab('show');
    }

    updatePortletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="portlet portlet-view 
        //              data-portlet-id='...'"></article>"
        container = $(container);
        let newContent = $(content);
        this._styleSemantics(newContent);
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
        } else {
            this._tabCounter += 1;
            let id = "portlet-tab-" + this._tabCounter;
            let tabs = $("#portalTabs");
            let tab = $('<li class="nav-item" data-portlet-tab="' + id + '"'
                + ' data-portlet-id="' + container.attr("data-portlet-id") + '">'
                + '<a class="nav-link" role="tab"'
                + ' id="' + id + '-tab" data-toggle="tab"'
                + ' href="#' + id + '-pane"'
                + ' alt="' + B4UIPortal.l10n.close + '"'
                + '>' + newContent.attr("data-portlet-title")
                + '<span class="fa fa-times ml-2 text-primary portlet-tab-close"></span>'
                + '</a>'
                + '</li>');
            tabs.append(tab);

            container.addClass("tab-pane fade m-3");
            container.attr("id", id + "-pane");
            container.attr("data-portlet-tab-tab", id + "-tab");
            container.attr("role", "tabpanel");
            container.attr("aria-labelledby", id + "-tab");
            container.append(newContent);
            let tabPanes = $("#portalTabPanes");
            tabPanes.append(container);
            this._layoutChanged();
        }
        if (foreground) {
            this._activatePortletView(container);
        }
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

    removePortletDisplays(containers) {
        containers.forEach(function(container) {
            container = $(container);
            if (container.hasClass('portlet-preview')) {
                let grid = $('#portalPreviews').data('gridstack');
                let gridItem = container.closest(".grid-stack-item");
                grid.removeWidget(gridItem);
            }
            if (container.hasClass('portlet-view')) {
                $("#portalOverviewTab").tab('show');
                var tabId = container.attr("data-portlet-tab-tab");
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
        $("#portalTabPanes > [data-portlet-id]").each(function(index) {
            let portletId = $(this).attr("data-portlet-id");
            tabsLayout.push(portletId);
        });

        this.sendLayout(previewLayout, tabsLayout, xtraInfo);
    };

    /**
     * Update the title of the portlet with the given id.
     *
     * @param {string} portletId the portlet id
     * @param {string} title the new title
     */
    updatePortletTitle(portletId, title) {
        let preview = $("#portalPreviews .portlet-preview[data-portlet-id='"
            + portletId + "'");
        if (preview.length > 0) {
            let portletHeader = preview.find(".portlet-preview-header");
            portletHeader.find(".portlet-preview-title").remove();
            portletHeader.prepend("<span class='portlet-preview-title flex-fill'>"
                + title + "</span>");
        }
        let view = $("#portalTabs .nav-item[data-portlet-id='" + portletId + "']");
        if (view.length > 0) {
            let titleNode = view.find(".nav-link");
            let close = titleNode.find(".portlet-tab-close").detach();
            titleNode.empty();
            titleNode.append(title);
            titleNode.append(close);
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
        container = $(container);
        let dialog = $('<div class="modal" tabindex="-1" role="dialog">'
            + '<div class="modal-dialog modal-lg" role="document">'
            + '<div class="modal-content">'
            + '<div class="modal-header">'
            + '<button type="button" class="close" data-dismiss="modal"'
            + ' alt="' + B4UIPortal.l10n.close + '">'
            + '<span aria-hidden="true">&times;</span>'
            + '</button></div>'
            + '<div class="modal-body portlet-content">'
            + '</div>'
            + '<div class="modal-footer">'
            + '<button type="button" class="btn btn-primary" data-dismiss="modal">'
            + B4UIPortal.l10n.ok + '</button>'
            + '</div>'
            + '</div>'
            + '</div>'
            + '</div>');
        dialog.find(".modal-body").append($(content));
        dialog.find(".btn-primary").on('click', function() {
            self.portal().execOnApply(container[0]);
        });
        container.append(dialog);
        dialog.modal();
    }
}
