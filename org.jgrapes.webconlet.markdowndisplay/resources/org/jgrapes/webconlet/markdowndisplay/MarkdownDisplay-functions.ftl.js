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
    
    JGConsole.registerConletFunction(
            "org.jgrapes.webconlet.markdowndisplay.MarkdownDisplayConlet",
            "updateAll", function(conletId, title, previewContent, viewContent, modes) {
                let conlet = JGConsole.instance.findConletPreview(conletId);
                if (conlet) {
                    JGConsole.instance.updateConletModes(conletId, modes);
                    let content = conlet.element()
                        .querySelector(".jgrapes-markdownconlet-content");
                    content.innerHTML = "";
                    content.insertAdjacentHTML("beforeend",
                            mdProc.render(previewContent));
                }
                conlet = JGConsole.instance.findConletView(conletId);
                if (conlet) {
                    let content = conlet.element()
                        .querySelector(".jgrapes-markdownconlet-content");
                    content.innerHTML = "";
                    content.insertAdjacentHTML("beforeend",
                            mdProc.render(viewContent));
                }
                JGConsole.instance.updateConletTitle(conletId, title);
            });

    function debounce (f) {
        if (f.hasOwnProperty("debounceTimer")) {
            clearTimeout(f.debounceTimer);
        }
        f.debounceTimer = setTimeout(f, 500);
    }
    
    orgJGrapesConletsMarkdownDisplay.init = function(content) {
        // Title
        let titleSource = content.querySelector('.jgrapes-conlet-mdp-title-input');

        // Preview
        let previewSource = content.querySelector('.jgrapes-conlet-mdp-preview-input');
        let previewPreview = content.querySelector('.jgrapes-conlet-mdp-preview-preview');
        let updatePreview = function() {
            let input = previewSource.value;
            let result = mdProc.render(input);
            previewPreview.innerHTML = result;
        }
        updatePreview();
        previewSource.addEventListener("keyup", () => debounce(updatePreview));

        // View
        let viewSource = content.querySelector('.jgrapes-conlet-mdp-view-input');
        let viewPreview = content.querySelector('.jgrapes-conlet-mdp-view-preview');
        let updateView = function() {
            let input = viewSource.value;
            let result = mdProc.render(input);
            viewPreview.innerHTML = result;
        }
        updateView();
        viewSource.addEventListener("keyup", () => debounce(updateView));
    }
    
    orgJGrapesConletsMarkdownDisplay.action = function(element) {
        let conletId = element.closest("[data-conlet-id]")
                .getAttribute("data-conlet-id");
        let titleSource = element.querySelector('.jgrapes-conlet-mdp-title-input');
        let previewSource = element.querySelector('.jgrapes-conlet-mdp-preview-input');
        let viewSource = element.querySelector('.jgrapes-conlet-mdp-view-input');
        JGConsole.notifyConletModel(conletId, "update", titleSource.value,
                previewSource.value, viewSource.value);
        return true;
    }
    
})();

