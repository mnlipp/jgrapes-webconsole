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

/**
 * @module jquiconsole
 */ 
export const JQUIConsole = {};
export default JQUIConsole;

var log = JGConsole.Log;

JQUIConsole.Renderer = class extends JGConsole.Renderer {

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
        let _this = this;
        log.debug("Locking screen");
        $("body").faLoading({
            icon: "fa-circle-o-notch",
            spin: true,
            text: false
        });

        // Top bar
        $("#theme-menu").on("click", "[data-theme-id]", function() {
            _this.send("setTheme", $(this).data("theme-id"));
            $("#theme-menu").jqDropdown("hide");
        });

        $("#language-menu").on("click", "[data-locale]", function() {
            _this.sendSetLocale($(this).data("locale"), true);
            $("#theme-menu").jqDropdown("hide");
        });

        $("#addon-menu").on("click", "[data-conlet-type]", function() {
            _this.sendAddConlet($(this).data("conlet-type"),
                $(this).data("render-modes"));
            $("#theme-menu").jqDropdown("hide");
        });

        // Tabs
        $("#conlet-tabs").tabs();

        // AddTab button
        var tabs = $("#conlet-tabs").tabs();

        // Close icon: removing the tab on click
        tabs.on("click", "span.ui-icon-close", function() {
            var panelId = $(this).closest("li").remove().attr("aria-controls");
            $("#" + panelId).remove();
            tabs.tabs("refresh");
            _this._layoutChanged();
        });

        // Dialogs
        $("#console-session-suspended-dialog").dialog({
            autoOpen: false,
            modal: true,
        });

        //
        // Conlet Grid
        //

        // Prepare columns
        $(".preview-area").sortable({
            handle: ".conlet-header",
            cancel: ".conlet-toggle",
            placeholder: "conlet-placeholder ui-corner-all",
            stop: function(event, ui) { _this._layoutChanged(); }
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
        $("#console-session-suspended-dialog").dialog("option", "buttons", {
            Ok: function() {
                $(this).dialog("close");
                resume();
            }
        });
        $("#console-session-suspended-dialog").dialog("open");
    }

    consoleConfigured() {
        this._layoutChanged();
        log.debug("Unlocking screen");
        $("body").faLoading('remove');
    }

    addConletType(conletType, displayNames, renderModes) {
        // Add to menu
        let lang = document.querySelector("html").getAttribute('lang');
        let displayName = JGConsole.forLang(displayNames, lang);
        let item = $('<li class="ui-menu-item">'
            + '<div class="ui-menu-item-wrapper" data-conlet-type="'
            + conletType + '">' + displayName + '</div></li>');
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
        if (isNew) {
            container.addClass('ui-widget ui-widget-content ui-helper-clearfix ui-corner-all');
            container.append('<div class="conlet-header ui-widget-header"><span class="conlet-header-text"></span></div>'
                + '<div class="conlet-content ui-widget-content"></div>');

            let conletHeader = container.find(".conlet-header");
            conletHeader.addClass("ui-widget-header ui-corner-all");
            this._setModeIcons(container, modes);
            let previewArea = $(".preview-area");
            let inserted = false;
            // Hack to check if conletId is in
            // lastPreviewLayout
            let conletId = container.attr("data-conlet-id");
            if (_this._isBefore(_this._lastPreviewLayout, conletId, conletId)) {
                previewArea.find(".conlet").each(
                    function(conletIndex) {
                        let item = $(this);
                        let itemId = item.attr("data-conlet-id");
                        if (!_this._isBefore(_this._lastPreviewLayout,
                            itemId, conletId)) {
                            item.before(conlet);
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
        let conletHeaderText = container.find(".conlet-header-text");
        conletHeaderText.text(newContent.attr("data-conlet-title"));
        let conletContent = container.find(".conlet-content");
        conletContent.children().detach();
        conletContent.append(newContent);
        if (foreground) {
            $("#conlet-tabs").tabs("option", "active", 0);
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

    _setModeIcons(conlet, modes) {
        let _this = this;
        let conletHeader = conlet.find(".conlet-header");
        conletHeader.find(".ui-icon").remove();
        if (modes.includes(RenderMode.Edit)) {
            conletHeader.prepend("<span class='ui-icon ui-icon-wrench conlet-edit'></span>");
            conletHeader.find(".conlet-edit").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                _this.sendRenderConlet(conletId, [RenderMode.Edit, RenderMode.Foreground]);
            });
        }
        if (modes.includes(RenderMode.DeleteablePreview)) {
            conletHeader.prepend("<span class='ui-icon ui-icon-delete conlet-delete'></span>");
            conletHeader.find(".conlet-delete").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                _this.sendDeleteConlet(conletId);
            });
        }
        if (modes.includes("View")) {
            conletHeader.prepend("<span class='ui-icon ui-icon-fullscreen conlet-expand'></span>");
            conletHeader.find(".conlet-expand").on("click", function() {
                let icon = $(this);
                let conletId = icon.closest(".conlet").attr("data-conlet-id");
                let conletView = _this.findConletView(conletId);
                if (conletView) {
                    _this._activateConletView($(conletView));
                } else {
                    _this.sendRenderConlet(conletId, ["View", "Foreground"]);
                }
            });
        }
    }

    _activateConletView(conlet) {
        let tabs = $("#conlet-tabs").tabs();
        let conletIndex = conlet.index();
        if (conletIndex) {
            tabs.tabs("option", "active", conletIndex - 1);
        }
    }

    updateConletView(isNew, container, modes, content, foreground) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        container = $(container);
        let newContent = $(content);
        if (!isNew) {
            container.children().detach();
            container.append(newContent);
        } else {
            let tabs = $("#conlet-tabs").tabs();
            this._tabCounter += 1;
            let id = "conlet-tabs-" + this._tabCounter;
            let li = $(this._tabTemplate.replace(/@\{href\}/g, "#" + id)
                .replace(/@\{label\}/g, newContent.attr("data-conlet-title")));
            let tabItems = tabs.find(".ui-tabs-nav");
            tabItems.append(li);
            container.attr("id", id);
            container.append(newContent);
            tabs.append(container);
            tabs.tabs("refresh");
            this._layoutChanged();
        }
        if (foreground) {
            this._activateConletView(container);
        }
    }

    removeConletDisplays(containers) {
        containers.forEach(function(container) {
            container = $(container);
            if (container.hasClass('conlet-view')) {
                let panelId = container.closest(".ui-tabs-panel").remove().attr("id");
                let tabs = $("#conlet-tabs").tabs();
                tabs.find("li[aria-controls='" + panelId + "']").remove();
                $("#conlet-tabs").tabs().tabs("refresh");
            }
            if (container.hasClass('conlet-preview')) {
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
            .find(".conlet-preview[data-conlet-id]").each(function() {
                previewLayout.push($(this).attr("data-conlet-id"));
            });
        let tabsLayout = [];
        let tabs = $("#conlet-tabs").tabs();
        tabs.find(".ui-tabs-nav .ui-tabs-tab").each(function(index) {
            if (index > 0) {
                let tabId = $(this).attr("aria-controls");
                let conletId = tabs.find("#" + tabId).attr("data-conlet-id");
                tabsLayout.push(conletId);
            }
        });
        let xtraInfo = {};

        this.sendLayout(previewLayout, tabsLayout, xtraInfo);
    };

    /**
     * Update the title of the conlet with the given id.
     *
     * @param {string} conletId the conlet id
     * @param {string} title the new title
     */
    updateConletTitle(conletId, title) {
        let tabs = $("#conlet-tabs").tabs();
        let conletView = tabs.find("> .conlet-view[data-conlet-id='" + conletId + "']");
        if (conletView.length > 0) {
            conletView.find("[data-conlet-title]").attr("data-conlet-title", title);
            let tabId = conletView.attr("id");
            let conletTab = tabs.find("a[href='#" + tabId + "']");
            conletTab.empty();
            conletTab.append(title);
        }
        let conletPreview = this.findConletPreview(conletId);
        if (conletPreview) {
            let headerText = $(conletPreview).find(".conlet-header-text");
            headerText.empty();
            headerText.append(title);
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
        container = $(container);
        container.addClass("conlet-content");
        let dialogContent = $(content);
        container.append(dialogContent);
        container.dialog({
            modal: true,
            width: "auto",
            close: function(event, ui) {
                _this.console().execOnApply(container[0]);
            }
        });
    }
}

JQUIConsole.tooltip = function(nodeSet) {
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
 * the console page.
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
        let _this = this;
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
            _this.close();
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
            _this._autoCloseTimer = setTimeout(function() {
                _this.close();
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
        let _this = this;
        if (this._autoCloseTimer) {
            clearTimeout(this._autoCloseTimer);
        }
        this._isOpen = false;
        this._hide(this.widget(), this.options.hide, function() {
            if (_this.options.destroyOnClose) {
                _this.destroy();
            }
        });
    },

    _destroy: function() {
        if (this._isOpen) {
            this.close();
        }
    }
});
