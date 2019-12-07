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

import Vue from "./lib/vue.esm.browser.js"

Vue.component('jgp-pulldown-menu', {
  props: {
    id: String,
    label: String,
    items: Array,
    l10n: Function,
    action: Function
  },
  data: function () {
    return {
      expanded: false
    }
  },
  methods: {
    toggle: function(event) {
        this.$lastEvent = event;
        this.expanded = !this.expanded;
    },
    translate: function(raw) {
        if (this.l10n) {
            return this.l10n(raw);
        }
        return raw;
    }
  },
  template: `
    <div v-bind:id="id" class="jgp-pulldown-menu pulldown-menu">
      <button v-bind:aria-expanded="expanded ? 'true' : 'false'" 
        v-on:click="toggle"><span v-html="label"></span></button>
      <ul role="menu">
      <template v-for="item in items">
        <li role="none"><a role="menuitem" href="#">{{ translate(item[0]) }}</a></li>
      </template>
      </ul>
    </div>
  `,
  mounted: function() {
    // Any click event that is not the one handled by this component
    // causes the menu to be closed.
    let self = this;
    document.addEventListener("click", function(event) {
        if (self.$lastEvent && event.target !== self.$lastEvent.target) {
            self.expanded = false;
        }
    });
}
})
