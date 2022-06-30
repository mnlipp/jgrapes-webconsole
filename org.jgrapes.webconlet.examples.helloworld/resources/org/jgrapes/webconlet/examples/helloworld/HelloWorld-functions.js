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

var orgJGrapesConletsWorldDemoConlet = {};

(function() {

    $("body").on("click", ".HelloWorld-view .HelloWorld-toggle",
            function(event) {
        let conletId = $(this).closest("[data-conlet-id]").attr("data-conlet-id");
        JGConsole.notifyConletModel(conletId, "toggleWorld");
    })

    JGConsole.registerConletFunction(
            "org.jgrapes.webconlet.examples.helloworld.HelloWorldConlet",
            "setWorldVisible", function(conletId, state) {
                let conlet = JGConsole.instance.findConletView(conletId);
                let image = $(conlet.element()).find(".helloWorldIcon");
                if (state) {
                    image.show();
                } else {
                    image.hide();
                }
            });

    orgJGrapesConletsWorldDemoConlet.onUnload = function(preview, isUpdate) {
        if (!isUpdate) {
            JGConsole.notification("World Removed!", { type: "warning",
                autoClose: 5000 });
        }
    }
    
})();

