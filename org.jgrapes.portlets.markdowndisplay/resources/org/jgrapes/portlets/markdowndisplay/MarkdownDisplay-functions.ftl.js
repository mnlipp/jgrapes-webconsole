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

var orgJGrapesPortletsMarkdownDisplay = {
};

(function() {

    let mdProc = window.markdownit()
        .use(markdownitAbbr)
        .use(markdownitContainer, 'warning')
        .use(markdownitDeflist)
        .use(markdownitEmoji)
        .use(markdownitFootnote)
        .use(markdownitIns)
        .use(markdownitMark)
        .use(markdownitSub)
        .use(markdownitSup);
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.portlets.markdowndisplay.MarkdownDisplayPortlet",
            "updateAll", function(portletId, params) {
                let portlet = JGPortal.findPortletPreview(portletId);
                if (portlet) {
                    JGPortal.updatePortletModes(portletId, params[3]);
                    let headerText = portlet.find(".portlet-header-text");
                    headerText.empty();
                    headerText.append(params[0]);
                    let content = portlet.find(".jgrapes-markdownportlet-content");
                    content.empty();
                    content.append(mdProc.render(params[1]));
                }
                portlet = JGPortal.findPortletView(portletId);
                if (portlet) {
                    JGPortal.updatePortletViewTitle(portletId, params[0]);
                    let content = portlet.find(".jgrapes-markdownportlet-content");
                    content.empty();
                    content.append(mdProc.render(params[2]));
                }
            });

    function debounce (f) {
        if (f.hasOwnProperty("debounceTimer")) {
            clearTimeout(f.debounceTimer);
        }
        f.debounceTimer = setTimeout(f, 500);
    }
    
    orgJGrapesPortletsMarkdownDisplay.init = function(dialog) {
        // Title
        let titleSource = dialog.find('.jgrapes-portlets-mdp-title-input');
        
        // Preview
        let previewSource = dialog.find('.jgrapes-portlets-mdp-preview-input');
        let previewPreview = dialog.find('.jgrapes-portlets-mdp-preview-preview');
        let updatePreview = function() {
            let input = previewSource.val();
            let result = mdProc.render(input);
            previewPreview.html(result);
        }
        updatePreview();
        previewSource.on("keyup", function() { debounce(updatePreview); });
        
        // View
        let viewSource = dialog.find('.jgrapes-portlets-mdp-view-input');
        let viewPreview = dialog.find('.jgrapes-portlets-mdp-view-preview');
        let updateView = function() {
            let input = viewSource.val();
            let result = mdProc.render(input);
            viewPreview.html(result);
        }
        updateView();
        viewSource.on("keyup", function() { debounce(updateView); });
        
        // Close action
        dialog.on("dialogclose", function(event) {
            let oet = event.originalEvent.target;
            if (oet.classList && oet.classList.contains("ui-dialog-titlebar-close")) {
                let portletId = $(event.target).attr("data-portlet-id");
                JGPortal.notifyPortletModel(portletId, "update", titleSource.val(),
                        previewSource.val(), viewSource.val());
            }
        })
    }
    
})();

