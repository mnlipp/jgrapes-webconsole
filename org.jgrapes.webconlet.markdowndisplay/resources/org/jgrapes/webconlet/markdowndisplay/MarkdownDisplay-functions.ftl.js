/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

var orgJGrapesConletsMarkdownDisplay = {
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
    
    JGConsole.registerConletMethod(
            "org.jgrapes.webconlet.markdowndisplay.MarkdownDisplayConlet",
            "updateAll", function(conletId, params) {
                let conlet = JGConsole.renderer.findConletPreview(conletId);
                if (conlet) {
                    JGConsole.renderer.updateConletModes(conletId, params[3]);
                    let content = $(conlet).find(".jgrapes-markdownconlet-content");
                    content.empty();
                    content.append(mdProc.render(params[1]));
                }
                conlet = JGConsole.renderer.findConletView(conletId);
                if (conlet) {
                    let content = $(conlet).find(".jgrapes-markdownconlet-content");
                    content.empty();
                    content.append(mdProc.render(params[2]));
                }
                JGConsole.renderer.updateConletTitle(conletId, params[0]);
            });

    function debounce (f) {
        if (f.hasOwnProperty("debounceTimer")) {
            clearTimeout(f.debounceTimer);
        }
        f.debounceTimer = setTimeout(f, 500);
    }
    
    orgJGrapesConletsMarkdownDisplay.init = function(content) {
        content = $(content);
        // Title
        let titleSource = content.find('.jgrapes-conlet-mdp-title-input');
        
        // Preview
        let previewSource = content.find('.jgrapes-conlet-mdp-preview-input');
        let previewPreview = content.find('.jgrapes-conlet-mdp-preview-preview');
        let updatePreview = function() {
            let input = previewSource.val();
            let result = mdProc.render(input);
            previewPreview.html(result);
        }
        updatePreview();
        previewSource.on("keyup", function() { debounce(updatePreview); });
        
        // View
        let viewSource = content.find('.jgrapes-conlet-mdp-view-input');
        let viewPreview = content.find('.jgrapes-conlet-mdp-view-preview');
        let updateView = function() {
            let input = viewSource.val();
            let result = mdProc.render(input);
            viewPreview.html(result);
        }
        updateView();
        viewSource.on("keyup", function() { debounce(updateView); });
    }
    
    orgJGrapesConletsMarkdownDisplay.apply = function(element) {
        element = $(element);
        let conletId = element.closest("[data-conlet-id]").attr("data-conlet-id");
        let titleSource = element.find('.jgrapes-conlet-mdp-title-input');
        let previewSource = element.find('.jgrapes-conlet-mdp-preview-input');
        let viewSource = element.find('.jgrapes-conlet-mdp-view-input');
        JGConsole.notifyConletModel(conletId, "update", titleSource.val(),
                previewSource.val(), viewSource.val());
    }
    
})();

