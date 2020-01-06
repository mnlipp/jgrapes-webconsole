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
 * @module jquiportal
 */ 
export const JQUIPortal = {};
export default JQUIPortal;

var log = JGPortal.log;

JQUIPortal.Renderer = class extends JGPortal.Renderer {

    constructor() {
        super();
        this._connectionLostNotification = null;
        this._lastPreviewLayout = [];
        this._lastTabsLayout = [];
        this._lastXtraInfo = {};
        this._tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
            "<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
        this._tabCounter = 1;
    }

    init() {
        let self = this;
        log.debug("Locking screen");
        $("body").faLoading({
            icon: "fa-circle-o-notch",
            spin: true,
            text: false
        });

        // Top bar
        $("#theme-menu").on("click", "[data-theme-id]", function() {
            self.send("setTheme", $(this).data("theme-id"));
            $("#theme-menu").jqDropdown("hide");
        });

        $("#language-menu").on("click", "[data-locale]", function() {
            self.sendSetLocale($(this).data("locale"), true);
            $("#theme-menu").jqDropdown("hide");
        });

        $("#addon-menu").on("click", "[data-portlet-type]", function() {
            self.sendAddPortlet($(this).data("portlet-type"),
                $(this).data("render-modes"));
            $("#theme-menu").jqDropdown("hide");
        });

        // Tabs
        $("#portlet-tabs").tabs();

        // AddTab button
        var tabs = $("#portlet-tabs").tabs();

        // Close icon: removing the tab on click
        tabs.on("click", "span.ui-icon-close", function() {
            var panelId = $(this).closest("li").remove().attr("aria-controls");
            $("#" + panelId).remove();
            tabs.tabs("refresh");
            self._layoutChanged();
        });

        // Dialogs
        $("#portal-session-suspended-dialog").dialog({
            autoOpen: false,
            modal: true,
        });

        //
        // Portlet Grid
        //

        // Prepare columns
        $(".preview-area").sortable({
            handle: ".portlet-header",
            cancel: ".portlet-toggle",
            placeholder: "portlet-placeholder ui-corner-all",
            stop: function(event, ui) { self._layoutChanged(); }
        });
    }

    connectionLost() {
        if (this._connectionLostNotification == null) {
            this._connectionLostNotification =
                $("#server-connection-lost-notification").notification({
                    error: true,
                    icon: "alert",
                    destroyOnClose: false,
                });
        } else {
            this._connectionLostNotification.notification("open");
        }
    }

    connectionRestored() {
        if (this._connectionLostNotification != null) {
            this._connectionLostNotification.notification("close");
        }
        $("#server-connection-restored-notification").notification({
            autoClose: 2000,
        });
    }

    connectionSuspended(resume) {
        $("#portal-session-suspended-dialog").dialog("option", "buttons", {
            Ok: function() {
                $(this).dialog("close");
                resume();
            }
        });
        $("#portal-session-suspended-dialog").dialog("open");
    }

    portalConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("body").faLoading('remove');
    }

    addPortletType(portletType, displayNames, renderModes) {
        // Add to menu
        let lang = document.querySelector("html").getAttribute('lang');
        let displayName = displayNames[lang];
        let item = $('<li class="ui-menu-item">'
            + '<div class="ui-menu-item-wrapper" data-portlet-type="'
            + portletType + '">' + displayName + '</div></li>');
        item.find("div").data("render-modes", renderModes);
        let inserted = false;
        $("#addon-menu-list").find(".ui-menu-item").each(function(index, el) {
            if (displayName < $(el).find(".ui-menu-item-wrapper").text()) {
                $(el).before(item);
                inserted = true;
                return false;
            }
        });
        if (!inserted) {
            $("#addon-menu-list").append(item);
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
        if (isNew) {
            container.addClass('ui-widget ui-widget-content ui-helper-clearfix ui-corner-all');
            container.append('<div class="portlet-header ui-widget-header"><span class="portlet-header-text"></span></div>'
                + '<div class="portlet-content ui-widget-content"></div>');

            let portletHeader = container.find(".portlet-header");
            portletHeader.addClass("ui-widget-header ui-corner-all");
            this._setModeIcons(container, modes);
            let previewArea = $(".preview-area");
            let inserted = false;
            // Hack to check if portletId is in
            // lastPreviewLayout
            let portletId = container.attr("data-portlet-id");
            if (self._isBefore(self._lastPreviewLayout, portletId, portletId)) {
                previewArea.find(".portlet").each(
                    function(portletIndex) {
                        let item = $(this);
                        let itemId = item.attr("data-portlet-id");
                        if (!self._isBefore(self._lastPreviewLayout,
                            itemId, portletId)) {
                            item.before(portlet);
                            inserted = true;
                            return false;
                        }
                    });
                if (!inserted) {
                    previewArea.append(container);
                }
                inserted = true;
            }
            if (!inserted) {
                previewArea.first().prepend(container);
            }
            this._layoutChanged();
        }
        let newContent = $(content);
        let portletHeaderText = container.find(".portlet-header-text");
        portletHeaderText.text(newContent.attr("data-portlet-title"));
        let portletContent = container.find(".portlet-content");
        portletContent.children().detach();
        portletContent.append(newContent);
        if (foreground) {
            $("#portlet-tabs").tabs("option", "active", 0);
        }
    }

    _isBefore(items, x, limit) {
        for (let i = 0; i < items.length; i++) {
            if (items[i] === x) {
                return true;
            }
            if (items[i] === limit) {
                return false;
            }
        }
        return false;
    }

    _setModeIcons(portlet, modes) {
        let self = this;
        let portletHeader = portlet.find(".portlet-header");
        portletHeader.find(".ui-icon").remove();
        if (modes.includes("Edit")) {
            portletHeader.prepend("<span class='ui-icon ui-icon-wrench portlet-edit'></span>");
            portletHeader.find(".portlet-edit").on("click", function() {
                let icon = $(this);
                let portletId = icon.closest(".portlet").attr("data-portlet-id");
                self.sendRenderPortlet(portletId, ["Edit", "Foreground"]);
            });
        }
        if (modes.includes("DeleteablePreview")) {
            portletHeader.prepend("<span class='ui-icon ui-icon-delete portlet-delete'></span>");
            portletHeader.find(".portlet-delete").on("click", function() {
                let icon = $(this);
                let portletId = icon.closest(".portlet").attr("data-portlet-id");
                self.sendDeletePortlet(portletId);
            });
        }
        if (modes.includes("View")) {
            portletHeader.prepend("<span class='ui-icon ui-icon-fullscreen portlet-expand'></span>");
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
        let tabs = $("#portlet-tabs").tabs();
        let portletIndex = portlet.index();
        if (portletIndex) {
            tabs.tabs("option", "active", portletIndex - 1);
        }
    }

    updatePortletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="portlet portlet-view 
        //              data-portlet-id='...'"></article>"
        container = $(container);
        let newContent = $(content);
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
        } else {
            let tabs = $("#portlet-tabs").tabs();
            this._tabCounter += 1;
            let id = "portlet-tabs-" + this._tabCounter;
            let li = $(this._tabTemplate.replace(/@\{href\}/g, "#" + id)
                .replace(/@\{label\}/g, newContent.attr("data-portlet-title")));
            let tabItems = tabs.find(".ui-tabs-nav");
            tabItems.append(li);
            container.attr("id", id);
            container.append(newContent);
            tabs.append(container);
            tabs.tabs("refresh");
            this._layoutChanged();
        }
        if (foreground) {
            this._activatePortletView(container);
        }
    }

    removePortletDisplays(containers) {
        containers.forEach(function(container) {
            container = $(container);
            if (container.hasClass('portlet-view')) {
                let panelId = container.closest(".ui-tabs-panel").remove().attr("id");
                let tabs = $("#portlet-tabs").tabs();
                tabs.find("li[aria-controls='" + panelId + "']").remove();
                $("#portlet-tabs").tabs().tabs("refresh");
            }
            if (container.hasClass('portlet-preview')) {
                container.remove();
            }
        });
        this._layoutChanged();
    }

    notification(content, options) {
        $(content).notification(options);
    }

    _layoutChanged() {
        let previewLayout = [];
        $(".overview-panel").find(".preview-area")
            .find(".portlet-preview[data-portlet-id]").each(function() {
                previewLayout.push($(this).attr("data-portlet-id"));
            });
        let tabsLayout = [];
        let tabs = $("#portlet-tabs").tabs();
        tabs.find(".ui-tabs-nav .ui-tabs-tab").each(function(index) {
            if (index > 0) {
                let tabId = $(this).attr("aria-controls");
                let portletId = tabs.find("#" + tabId).attr("data-portlet-id");
                tabsLayout.push(portletId);
            }
        });
        let xtraInfo = {};

        this.sendLayout(previewLayout, tabsLayout, xtraInfo);
    };

    /**
     * Update the title of the portlet with the given id.
     *
     * @param {string} portletId the portlet id
     * @param {string} title the new title
     */
    updatePortletTitle(portletId, title) {
        let tabs = $("#portlet-tabs").tabs();
        let portletView = tabs.find("> .portlet-view[data-portlet-id='" + portletId + "']");
        if (portletView.length > 0) {
            portletView.find("[data-portlet-title]").attr("data-portlet-title", title);
            let tabId = portletView.attr("id");
            let portletTab = tabs.find("a[href='#" + tabId + "']");
            portletTab.empty();
            portletTab.append(title);
        }
        let portletPreview = this.findPortletPreview(portletId);
        if (portletPreview) {
            let headerText = $(portletPreview).find(".portlet-header-text");
            headerText.empty();
            headerText.append(title);
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
        container.addClass("portlet-content");
        let dialogContent = $(content);
        container.append(dialogContent);
        container.dialog({
            modal: true,
            width: "auto",
            close: function(event, ui) {
                self.portal().execOnApply(container[0]);
            }
        });
    }
}

JQUIPortal.tooltip = function(nodeSet) {
    nodeSet.tooltip({
        items: "[data-tooltip-id], [title]",
        content: function() {
            let element = $(this);
            if (element.is("[data-tooltip-id]")) {
                let id = element.attr("data-tooltip-id");
                let tooltip = $("#" + id);
                if (tooltip) {
                    tooltip = tooltip.clone(true);
                    tooltip.removeClass("jgrapes-tooltip-prototype")
                    return tooltip;
                }
                return "<#" + id + ">";
            }
            if (element.is("[title]")) {
                return element.attr("title");
            }
        }
    });
}

/**
 * A jQuery UI plugin for displaying notifications on the top of 
 * the portal page.
 * 
 * @name NotificationPlugin
 * @namespace NotificationPlugin
 */
var notificationContainer = null;
var notificationCounter = 0;

$(function() {
    let top = $("<div></div>");
    notificationContainer = $("<div class='ui-notification-container ui-front'></div>");
    top.append(notificationContainer);
    $("body").prepend(top);
});

$.widget("ui.notification", {

    /**
     * <dl>
     * <dt>error {Boolean}</dt>
     *   <dd>Indicates that the notification is to be display as error.</dd>
     * <dt>autoClose {integer}</dt>
     *   <dd>If specified, closes the botification automatically after
     *   the given number of milliseconds.</dd>
     * <dt>destroyOnClose {Boolean}</dt>
     *   <dd>Indicates that the widget should be destroyed if the
     *   notification is closed.</dd>
     * <dt>show {Object}</dt>
     *   <dd>If and how to animate the showing of the notification
     *   (see the <a href="https://api.jqueryui.com/jQuery.widget/#option-show">
     *   jQuery UI</a> documentation). Defaults to: <code>
     *   { effect: "blind", direction: "up", duration: "fast", }</code></dd>
     * <dt>hide {Object}</dt>
     *   <dd>If and how to animate the showing of the notification
     *   (see the <a href="https://api.jqueryui.com/jQuery.widget/#option-hide">
     *   jQuery UI</a> documentation). Defaults to: <code>
     *   { effect: "blind", direction: "up", duration: "fast", }</dd>
     * </dl>
     * 
     * @memberof NotificationPlugin
     */
    options: {
        classes: {
            "ui-notification": "ui-corner-all ui-widget-shadow",
            "ui-notification-content": "ui-corner-all",
        },
        error: false,
        icon: "circle-info",
        autoOpen: true,
        autoClose: null,
        destroyOnClose: null,
        show: {
            effect: "blind",
            direction: "up",
            duration: "fast",
        },
        hide: {
            effect: "blind",
            direction: "up",
            duration: "fast",
        },
    },

    _create: function() {
        let self = this;
        this._isOpen = false;
        if (this.options.destroyOnClose === null) {
            this.options.destroyOnClose = this.options.autoOpen;
        }
        // Options are already merged and stored in this.options
        let widget = $("<div></div>");
        let notificationId = "ui-notification-" + ++notificationCounter;
        widget.attr("id", notificationId);
        this._addClass(widget, "ui-notification");
        widget.addClass("ui-widget-content");
        widget.addClass("ui-widget");
        if (this.options.error) {
            widget.addClass("ui-state-error");
        } else {
            widget.addClass("ui-state-highlight");
        }

        // Close button (must be first for overflow: hidden to work).
        let button = $("<button></button>");
        button.addClass("ui-notification-close");
        button.button({
            icon: "ui-icon-close",
            showLabel: false,
        });
        button.on("click", function() {
            self.close();
        });
        widget.append(button);

        // Prepend icon
        if (this.options.icon) {
            widget.append($('<span class="ui-notification-content ui-icon' +
                ' ui-icon-' + this.options.icon + '"></span>'));
        }
        let widgetContent = $("<div></div>");
        this._addClass(widgetContent, "ui-notification-content");
        widget.append(widgetContent);
        widgetContent.append(this.element.clone());
        widgetContent.find(":first-child").show();
        widget.hide();

        // Optional auto close
        this._autoCloseTimer = null;
        if (this.options.autoClose) {
            self._autoCloseTimer = setTimeout(function() {
                self.close();
            }, this.options.autoClose);
        }

        // Add to container
        this.notification = widget;

        // Open if desired
        if (this.options.autoOpen) {
            this.open();
        }
    },

    open: function() {
        notificationContainer.prepend(this.widget());
        this._isOpen = true;
        this._show(this.widget(), this.options.show);
    },

    widget: function() {
        return this.notification;
    },

    close: function() {
        let self = this;
        if (this._autoCloseTimer) {
            clearTimeout(this._autoCloseTimer);
        }
        this._isOpen = false;
        this._hide(this.widget(), this.options.hide, function() {
            if (self.options.destroyOnClose) {
                self.destroy();
            }
        });
    },

    _destroy: function() {
        if (this._isOpen) {
            this.close();
        }
    }
});
