/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2019  Michael N. Lipp
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

import Vue from "../../page-resource/vue/vue.esm.browser.js"

window.orgJGrapesOsgiConletStyleTest = {};

window.orgJGrapesOsgiConletStyleTest.initView = function(content) {
    let cont = $(content).find(".jgrapes-osgi-style-test-treegrid-table");
    new Vue({
        el: $(cont)[0],
        data: {
            conletId: $(content).closest("[data-conlet-id]").data("conlet-id"),
            controller: new JGConsole.TableController([
                ["year", 'Year'],
                ["month", 'Month'],
                ["title", 'Title'],
                ], {
                sortKey: "year"
            }),
            detailsByKey: {},
            issues: [
                { year: 2019, month: 6, title: "Middle of year" },
                { year: 2019, month: 12, title: "End of year" },
                { year: 2020, month: 1, title: "A new year begins" },
            ],
        },
        computed: {
            filteredData: function() {
                let infos = Object.values(this.issues);
                return this.controller.filter(infos);
            }
        },
        methods: {
            toggleDetails: function(issue) {
                let key = [issue.year, issue.month];
                if (key in this.detailsByKey) {
                    Vue.delete(this.detailsByKey, key);
                    return;
                }
                Vue.set(this.detailsByKey, key, true);
            },
            expanded: function(issue) {
                let key = [issue.year, issue.month];
                return (key in this.detailsByKey);                
            }
        }
    });
}

window.orgJGrapesOsgiConletStyleTest.onUnload = function(content) {
    if ("__vue__" in content) {
        content.__vue__.$destroy();
        return;
    }
    for (let child of content.children) {
        window.orgJGrapesOsgiConletStyleTest.onUnload(child);
    }
}
