/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
var B4UIPortal = {
};

(function () {

    var log = JGPortal.log;
    
    B4UIPortal.Renderer = class extends JGPortal.Renderer {
        
        constructor() {
            super();
//            this._connectionLostNotification = null;
//            this._lastPreviewLayout = [];
//            this._lastTabsLayout = [];
//            this._lastXtraInfo = {};
//            this._tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
//                "<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
            this._tabCounter = 1;
        }
        
        init() {
            let self = this;
//            log.debug("Locking screen");
//            $("body").faLoading({
//                icon: "fa-circle-o-notch",
//                spin: true,
//                text: false
//            });
          
//            // Top bar
//            $( "#theme-menu" ).on("click", "[data-theme-id]", function() {
//                self.send("setTheme", $(this).data("theme-id"));
//              $( "#theme-menu" ).jqDropdown("hide");
//            });
//            
//            $( "#language-menu" ).on("click", "[data-locale]", function() {
//                self.sendSetLocale($(this).data("locale"));
//                $( "#theme-menu" ).jqDropdown("hide");
//            });
//            
//            $( "#addon-menu" ).on("click", "[data-portlet-type]", function() {
//                self.sendAddPortlet($(this).data("portlet-type"));
//                $( "#theme-menu" ).jqDropdown("hide");
//            });
//            
            // Tabs
            var tabs = $( "#portalTabs" );
            // Work around https://github.com/twbs/bootstrap/issues/27006
//            tabs.on("shown.bs.tab", function(event) {
//                let shown = $(event.target);
//                if (!shown.hasClass("nav-link")) {
//                    shown.removeClass("active show");
//                    shown.closest(".nav-link").addClass("active show");
//                }
//            });

            // Close icon: removing the tab on click
            tabs.on("click", ".portlet-tab-close", function() {
              var tabId = $( this ).closest( "li" ).remove().attr( "data-portlet-tab" );
              $( "#" + tabId + "-pane" ).remove();
              $("#portalOverviewTab").tab('show');
              self._layoutChanged();
            });

//            // Dialogs
//            $( "#portal-session-suspended-dialog" ).dialog({
//                autoOpen: false,
//                modal: true,
//            });
//
//            //
//            // Portlet Grid
//            //
//            
//            // Prepare columns
//            $( ".preview-area" ).sortable({
//              handle: ".portlet-header",
//              cancel: ".portlet-toggle",
//              placeholder: "portlet-placeholder ui-corner-all",
//              stop: function( event, ui ) { self._layoutChanged(); }
//            });
        }
        
        connectionLost() {
//            if (this._connectionLostNotification == null) {
//                this._connectionLostNotification = 
//                    $( "#server-connection-lost-notification" ).notification({
//                        error: true,
//                        icon: "alert",
//                        destroyOnClose: false,
//                    });
//            } else {
//                this._connectionLostNotification.notification( "open" );
//            }
        }
        
        connectionRestored() {
//            if (this._connectionLostNotification != null) {
//                this._connectionLostNotification.notification( "close" );
//            }
//            $( "#server-connection-restored-notification" ).notification({
//                autoClose: 2000,
//            });
        }

        connectionSuspended(resume) {
//            $("#portal-session-suspended-dialog").dialog("option", "buttons", {
//                Ok : function() {
//                    $(this).dialog("close");
//                    resume();
//                }
//            });
//            $("#portal-session-suspended-dialog").dialog("open");
        }
        
        portalConfigured() {
            this._layoutChanged();
//            log.debug("Unlocking screen");
//            $("body").faLoading('remove');
        }
        
        addPortletType(portletType, displayName, isInstantiable) {
            // Add to menu
            let self = this;
            let item = $('<a class="dropdown-item" href="#" data-portlet-type="' 
                    + portletType + '">' + displayName + '</a>');
            item.on('click', function(e) {
                self.sendAddPortlet($(this).data("portlet-type"));
            });
            $("#portalNavbarPortletList").append(item);
        }
        
        lastPortalLayout(previewLayout, tabsLayout, xtraInfo) {
//            this._lastPreviewLayout = previewLayout;
//            this._lastTabsLayout = tabsLayout;
//            this._lastXtraInfo = xtraInfo;
        }

        updatePortletPreview(isNew, container, modes, content, foreground) {
            // Container is "<div class="portlet portlet-preview data-portlet-id='...'"></div>"
            let self = this;
            if (isNew) {
                container.addClass('card');
                container.append('<h6 class="card-header portlet-preview-header">'
                        + '<span class="portlet-preview-title"></span></h6>'
                        + '<div class="card-body portlet-preview-content"></div>');
                this._setModeIcons(container, modes);
                
                // Put into grid item wrapper
                let gridItem = $('<div class="grid-stack-item" data-gs-auto-position="1"'
                        + ' data-gs-width="4" data-gs-height="4">'
                        + '</div>');
                container.addClass('grid-stack-item-content');
                gridItem.append(container);
                
                // Finally add to grid
                let grid = $('#portalPreviews').data('gridstack');
                grid.addWidget(gridItem);
                
                this._layoutChanged();
            }
            let newContent = $(content);
            let portletTitle = container.find(".portlet-preview-title");
            portletTitle.text(newContent.attr("data-portlet-title"));
            let previewContent = container.find(".portlet-preview-content");
            previewContent.empty();
            previewContent.append(newContent);
            if (foreground) {
                $("#portalOverviewTab").tab('show');
            }
        }
        
//        _isBefore(items, x, limit) {
//            for (let i = 0; i < items.length; i++) {
//                if (items[i] === x) {
//                    return true;
//                }
//                if (items[i] === limit) {
//                    return false;
//                }
//            }
//            return false;
//        }
        
        _setModeIcons(portlet, modes) {
            let self = this;
            let portletHeader = portlet.find( ".portlet-preview-header" );
            portletHeader.find( ".portlet-preview-icon" ).remove();
//            if (modes.includes("Edit")) {
//                portletHeader.prepend( "<span class='ui-icon ui-icon-wrench portlet-edit'></span>");
//                portletHeader.find(".portlet-edit").on( "click", function() {
//                    let icon = $( this );
//                    let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
//                    self.sendRenderPortlet(portletId, "Edit", true);
//                });
//            }
            if (modes.includes("DeleteablePreview")) {
                portletHeader.prepend( "<a href='#' class='" + 
                        "float-right mr-2 portlet-preview-icon portlet-delete" +
                        " fa fa-times' role='button'></a>");
                portletHeader.find(".portlet-delete").on( "click", function() {
                    let icon = $( this );
                    let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
                    self.sendDeletePortlet(portletId);
                });
            }
            if (modes.includes("View")) {
                portletHeader.prepend( "<a href='#' class='" + 
                        "float-right portlet-preview-icon portlet-expand" +
                        " fa fa-expand' role='button'></a>");
                portletHeader.find(".portlet-expand").on( "click", function() {
                    let icon = $( this );
                    let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
                    let portletView = self.findPortletView(portletId);
                    if(portletView) { 
                        self._activatePortletView(portletView);
                    } else {
                        self.sendRenderPortlet(portletId, "View", true);
                    }
                });
            }
        }
        
        _activatePortletView(portlet) {
            let tabId = portlet.attr("data-portlet-tab-tab");
            $("#" + tabId).tab('show');
        }
        
        updatePortletView(isNew, container, modes, content, foreground) {
            // Container is "<div class="portlet portlet-view data-portlet-id='...'"></div>"

            let newContent = $(content);
            if (!isNew) {
                container.children().detach();
                container.append(newContent);
            } else {
                this._tabCounter += 1;
                let id = "portlet-tab-" + this._tabCounter;
                let tabs = $( "#portalTabs" );
                let tab = $('<li class="nav-item" data-portlet-tab="' + id + '">'
                        + '<a class="nav-link" role="tab"'
                        + ' id="' + id + '-tab" data-toggle="tab"' 
                        + ' href="#' + id + '-pane"'
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
                let tabPanes = $( "#portalTabPanes" );
                tabPanes.append( container );
                this._layoutChanged();
            }
            if (foreground) {
                this._activatePortletView(container);
            }
        }
        
        portletRemoved(containers) {
            containers.forEach(function(container) {
                container = $( container );
                if(container.hasClass('portlet-preview')) {
                    let grid = $('#portalPreviews').data('gridstack');
                    let gridItem = container.closest(".grid-stack-item");
                    grid.removeWidget(gridItem);
                }
                if(container.hasClass('portlet-view')) {
                    $("#portalOverviewTab").tab('show');
                    var tabId = container.attr("data-portlet-tab-tab"); 
                    $( "#" + tabId ).closest( "li" ).remove();
                    container.remove();
                }
            }); 
            this._layoutChanged();
        }
        
        notification(content, options) {
//            $(content).notification( options );
        }
        
        _layoutChanged() {
            let previewLayout = [];
            $("#portalOverviewPane").find("div.portlet[data-portlet-id]")
                .each(function() {
                      previewLayout.push($(this).attr("data-portlet-id"));
                });
            let tabsLayout = [];
            $("#portalTabPanes > [data-portlet-id]").each(function(index) {
                let portletId = $(this).attr("data-portlet-id");
                tabsLayout.push(portletId);
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
        updatePortletViewTitle(portletId, title) {
//            let tabs = $( "#portlet-tabs" ).tabs();
//            let portlet = tabs.find("> div[data-portlet-id='" + portletId + "']" );
//            if (portlet.length === 0) {
//                return;
//            }
//            portlet.find("[data-portlet-title]").attr("data-portlet-title", title);
//            let tabId = portlet.attr("id");
//            let portletTab = tabs.find("a[href='#" + tabId + "']");
//            portletTab.empty();
//            portletTab.append(title);
        }
      
        /**
         * Update the modes of the portlet with the given id.
         * 
         * @param {string} portletId the portlet id
         * @param {string[]} modes the modes
         */
        updatePortletModes(portletId, modes) {
//            let portlet = this.findPortletPreview(portletId);
//            if (!portlet) {
//                return;
//            }
//            this._setModeIcons(portlet, modes);
        }
        
        showEditDialog(container, modes, content) {
//            let dialogContent = $(content);
//            container.append(dialogContent);
//            container.dialog({
//                modal: true,
//                width: "auto",
//                close: function( event, ui ) {
//                    dialogContent.trigger("JGrapes.dialogClosed", [ container ]);
//                }
//            });
        }   
    }

})();

/**
 * A jQuery UI plugin for displaying notifications on the top of 
 * the portal page.
 * 
 * @name NotificationPlugin
 * @namespace NotificationPlugin
 */
(function() {
    var notificationContainer = null;
    var notificationCounter = 0;
    
    $( function() {
        let top = $( "<div></div>" );
        notificationContainer = $( "<div class='ui-notification-container ui-front'></div>" );
        top.append(notificationContainer);
        $( "body" ).prepend(top);
    });
    
    $.widget( "ui.notification", {
        
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
            let widget = $( "<div></div>" );
            let notificationId = "ui-notification-" + ++notificationCounter;
            widget.attr("id", notificationId);
            this._addClass( widget, "ui-notification" );
            widget.addClass( "ui-widget-content" );
            widget.addClass( "ui-widget" );
            if (this.options.error) {
                widget.addClass( "ui-state-error" );
            } else {
                widget.addClass( "ui-state-highlight" );
            }
            
            // Close button (must be first for overflow: hidden to work).
            let button = $( "<button></button>");
            button.addClass( "ui-notification-close" );
            button.button({
                icon: "ui-icon-close",
                showLabel: false,
            });
            button.on ( "click", function() {
                self.close();
            } );
            widget.append(button);
            
            // Prepend icon
            if (this.options.icon) {
                widget.append( $( '<span class="ui-notification-content ui-icon' +
                        ' ui-icon-' + this.options.icon + '"></span>' ) );
            }
            let widgetContent = $( "<div></div>" );
            this._addClass( widgetContent, "ui-notification-content" );
            widget.append(widgetContent);
            widgetContent.append( this.element.clone() );
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
            notificationContainer.prepend( this.widget() );
            this._isOpen = true;
            this._show( this.widget(), this.options.show );
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
            this._hide( this.widget(), this.options.hide, function() {
                if (self.options.destroyOnClose) {
                    self.destroy();
                }
            } );
        },
        
        _destroy: function() {
            if (this._isOpen) {
                this.close();
            }
        }
    });
})();