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
        l10n: {}
};

(function () {

    var log = JGPortal.log;
    
    B4UIPortal.Renderer = class extends JGPortal.Renderer {
        
        constructor() {
            super();
            this._connectionLostNotification = null;
//            this._lastPreviewLayout = [];
//            this._lastTabsLayout = [];
//            this._lastXtraInfo = {};
//            this._tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
//                "<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
            this._tabCounter = 1;
        }
        
        init() {
            let self = this;
            log.debug("Locking screen");
            
//            $( "#language-menu" ).on("click", "[data-locale]", function() {
//                self.sendSetLocale($(this).data("locale"));
//                $( "#theme-menu" ).jqDropdown("hide");
//            });
//            
            // Tabs
            var tabs = $( "#portalTabs" );

            // Close icon: removing the tab on click
            tabs.on("click", ".portlet-tab-close", function() {
              var tabId = $( this ).closest( "li" ).remove().attr( "data-portlet-tab" );
              $( "#" + tabId + "-pane" ).remove();
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
                this._connectionLostNotification.alert( "close" );
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
            // Container is:
            //     <div class='portlet portlet-preview' data-portlet-id='...' 
            //     data-portlet-grid-columns='...' data-portlet-grid-rows='   '></div>"
            let self = this;
            let newContent = $(content);
            if (isNew) {
                container.addClass('card');
                container.append('<h6 class="card-header portlet-preview-header">'
                        + '<span class="portlet-preview-title"></span></h6>'
                        + '<div class="card-body portlet-preview-content"></div>');
                this._setModeIcons(container, modes);
                
                // Put into grid item wrapper
                let gridItem = $('<div class="grid-stack-item" data-gs-auto-position="1"'
                        + ' data-gs-width="4" data-gs-height="4" role="gridcell">'
                        + '</div>');
                let gridHint = newContent.attr("data-portlet-grid-columns");
                if (gridHint) {
                    gridItem.attr("data-gs-width", gridHint);
                }
                gridHint = newContent.attr("data-portlet-grid-rows");
                if (gridHint) {
                    gridItem.attr("data-gs-height", gridHint);
                }
                container.addClass('grid-stack-item-content');
                gridItem.append(container);
                
                // Finally add to grid
                let grid = $('#portalPreviews').data('gridstack');
                grid.addWidget(gridItem);
                
                this._layoutChanged();
            }
            let portletTitle = container.find(".portlet-preview-title");
            portletTitle.text(newContent.attr("data-portlet-title"));
            let previewContent = container.find(".portlet-preview-content");
            this._styleSemantics(newContent);
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
            if (modes.includes("Edit")) {
                portletHeader.prepend( "<a href='#' class='" + 
                        "float-right mr-2 portlet-preview-icon portlet-edit" +
                        " fa fa-wrench' role='button'></a>");
                portletHeader.find(".portlet-edit").on( "click", function() {
                    let icon = $( this );
                    let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
                    self.sendRenderPortlet(portletId, "Edit", true);
                });
            }
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
            this._styleSemantics(newContent);
            if (!isNew) {
                container.children().detach();
                container.append(newContent);
            } else {
                this._tabCounter += 1;
                let id = "portlet-tab-" + this._tabCounter;
                let tabs = $( "#portalTabs" );
                let tab = $('<li class="nav-item" data-portlet-tab="' + id + '"'
                        + ' data-portlet-id="' + container.attr("data-portlet-id") + '">'
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
        
        _styleSemantics(content) {
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
            let parsed = $( "<div>" + content + "</div>");
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
            let previewLayout = [];
            $("#portalOverviewPane").find(".portlet-preview[data-portlet-id]")
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
        updatePortletTitle(portletId, title) {
            let preview = $("#portalPreviews .portlet-preview[data-portlet-id='"
                    + portletId + "'");
            if (preview.length > 0) {
                let titleNode = preview.find(".portlet-preview-title");
                titleNode.empty();
                titleNode.append(title);
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
            let dialogContent = $(content);
            container.append(dialogContent);
            let dialog = $('<div class="modal" tabindex="-1" role="dialog">'
                    + '<div class="modal-dialog modal-lg" role="document">'
                    + '<div class="modal-content">'
                    + '<div class="modal-header">'
                    + '<button type="button" class="close" data-dismiss="modal" aria-label="Close">'
                    + '<span aria-hidden="true">&times;</span>'
                    + '</button></div>'
                    + '<div class="modal-body">'
                    + '</div>'
                    + '<div class="modal-footer">'
                    + '<button type="button" class="btn btn-primary" data-dismiss="modal">'
                    + B4UIPortal.l10n.ok + '</button>'
                    + '</div>'
                    + '</div>'
                    + '</div>'
                    + '</div>');
            dialog.find(".modal-body").append($(container));
            dialog.find(".btn-primary").on('click', function() {
                dialogContent.trigger("JGrapes.editDialog.apply", [ container ]);
            });
            dialog.modal();
        }   
    }

})();
