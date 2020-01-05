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
        if (typeof(raw) === "function") {
            return raw();
        }
        if (this.l10n) {
            return this.l10n(raw);
        }
        return raw;
    }
  },
  computed: {
    sortedItems: function() {
        let result = [];
        for (let item of this.items) {
            result.push([this.translate(item[0]), item]);
        }
        result.sort(function(a, b) {
            return a[0].localeCompare(b[0]);
        });
        return result;
    }    
  },
  template: `
    <div v-bind:id="id" class="jgp-pulldown-menu pulldown-menu">
      <button type="button" aria-haspopup="true"
        v-bind:aria-controls="id + '-menu'" 
        v-bind:aria-expanded="expanded ? 'true' : 'false'" 
        v-on:click="toggle"><span v-html="label"></span></button>
      <ul v-bind:id="id + 'menu'" role="menu">
      <template v-for="item in sortedItems">
        <li role="none"><button type="button" 
          role="menuitem" v-on:click="action(item[1])"
          >{{ item[0] }}</button></li>
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
});

Vue.component('jpg-tablist', {
  props: {
    id: String,
    initialTabs: Array,
    l10n: Function,
  },
  data: function () {
    return {
      tabs: this.initialTabs || [],
      selected: null
    }
  },
  methods: {
    label: function(tab) {
        if (tab.l10n) {
            return tab.l10n(tab.label);
        }
        if (this.l10n) {
            return this.l10n(tab.label);
        }
        return tab.label;
    },
    setupTabpanel: function(tabpanel) {
        tabpanel.setAttribute("role", "tabpanel");
        tabpanel.setAttribute("aria-labelledby", 
            tabpanel.getAttribute('id') + '-tab');
        if (tabpanel.getAttribute('id') === this.selected) {
            tabpanel.removeAttribute("hidden");
        } else {
            tabpanel.setAttribute("hidden", "");
        }
    },
    addTab(tab) {
        this.tabs.push(tab);
    },
    removeTab(tabId) {
        let prevTab = 0;
        for (let i in this.tabs) {
            if (this.tabs[i].id === tabId) {
                this.tabs.splice(i, 1);
                break;
            }
            prevTab = i;
        }
        if (this.tabs.length > 0) {
            this.selectTab(this.tabs[prevTab].id);
        }
    },
    selectTab: function(id) {
        if (this.selected) {
            let tabpanel = document.querySelector("#" + this.selected);
            if (tabpanel) {
                tabpanel.setAttribute("hidden", "");
            }
        }
        this.selected=id;
        let tabpanel = document.querySelector("#" + this.selected);
        if (tabpanel) {
            tabpanel.removeAttribute("hidden");
        }
    }
  },
  watch: {
    tabs: function(newValue) {
        if (this.selected === null && newValue.length > 0) {
            this.selectTab(this.tabs[0].id);
        }
    },
  },
  template: `
    <div v-bind:id="id" class="jgp-tablist" role="tablist">
      <span v-for="tab of tabs" role="group"
          v-bind:aria-selected="tab.id == selected ? 'true' : 'false'"
          v-bind:aria-controls="tab.id">
        <button type="button"  
          v-bind:id="tab.id + '-tab'" role="tab" 
          v-on:click="selectTab(tab.id)">{{ label(tab) }}</button><button
        type="button" v-if="tab.removeCallback" class="fa fa-times"
          v-on:click="tab.removeCallback()"></button>
      </span>
    </div>
  `,
  mounted: function() {
    if (this.tabs.length > 0) {
        this.selected = this.tabs[0].id;
    }
    for (let tab of this.tabs) {
        let tabpanel = document.querySelector("#" + tab.id);
        if (tabpanel == null) {
            continue;
        }
        this.setupTabpanel(tabpanel);
    }
  }
});

