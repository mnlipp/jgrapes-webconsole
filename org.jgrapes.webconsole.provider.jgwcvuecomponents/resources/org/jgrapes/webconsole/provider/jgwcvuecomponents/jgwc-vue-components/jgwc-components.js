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

import Vue from "../vue/vue.esm.browser.js"
import JGConsole from "../../console-base-resource/jgconsole.js"

JGConsole.jgwc = {};

JGConsole.jgwc.destroyVMs = function(content) {
    if ("__vue__" in content) {
        content.__vue__.$destroy();
        return;
    }
    for (let child of content.children) {
        JGConsole.jgwc.destroyVMs(child);
    }
}

const keys = {
    end: 35,
    home: 36,
    left: 37,
    up: 38,
    right: 39,
    down: 40,
    delete: 46,
    enter: 13,
    space: 32
};

Vue.jgwc = {};
Vue.prototype.jgwc = {};

var htmlRoot = document.querySelector("html");
var jgwcObserved = Vue.observable({
    lang: htmlRoot.getAttribute('lang')
});
Vue.prototype.jgwc.observed = jgwcObserved;

new MutationObserver(function(mutations) {
    mutations.forEach((mutation) => {
        if (mutation.type === 'attributes'
            && mutation.attributeName === 'lang') {
            jgwcObserved.lang = htmlRoot.getAttribute('lang');
        }
    });    
}).observe(htmlRoot, {
  childList: false,
  attributes: true,
  subtree: false
});


var scopeCounter = 1;

var jgwcIdScopeMixin = {
    created: function() {
        this._idScope = scopeCounter++;
    },
    methods: {
        scopedId: function(id) {
            return "jgwc-id-" + this._idScope + "-" + id;
        }
    }
};

/*
Vue.component('jgwc-id-scope', {
  data: function() {
    return {
        scope: "test"
    }
  },
  methods: {
    testIt: function() {
        return "Test";
    }
  },
  render: function() {
    return this.$slots.default;
  },
});
*/

Vue.component('jgwc-pulldown-menu', {
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
    <div v-bind:id="id" class="jgwc-pulldown-menu pulldown-menu">
      <button type="button" aria-haspopup="true"
        v-bind:aria-controls="id + '-menu'" 
        v-bind:aria-expanded="expanded ? 'true' : 'false'" 
        v-on:click="toggle"><span v-html="label"></span></button>
      <ul v-bind:id="id + '-menu'" role="menu">
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
    this._globalClickHandler = function(event) {
        if (self.$lastEvent && event.target !== self.$lastEvent.target) {
            self.expanded = false;
        }
    };
    document.addEventListener("click", this._globalClickHandler);
  },
  beforeDestroy: function() {
    document.removeEventListener("click", this._globalClickHandler);
  }
});

Vue.component('jgwc-tablist', {
  props: {
    id: {
      type: String,
      required: true,
    },
    initialPanels: Array,
    l10n: Function,
  },
  data: function () {
    return {
      panels: this.initialPanels || [],
      selected: null
    }
  },
  computed: {
    isVertical: function() {
        return this.$attrs["aria-orientation"] !== undefined
            && this.$attrs["aria-orientation"] === "vertical";
    }
  },
  methods: {
    addPanel: function(panel) {
        this.panels.push(panel);
        this._setupTabpanel(panel)
    },
    removePanel: function(panelId) {
        let prevPanel = 0;
        for (let i in this.panels) {
            if (this.panels[i].id === panelId) {
                this.panels.splice(i, 1);
                break;
            }
            prevPanel = i;
        }
        if (this.panels.length > 0) {
            this.selectPanel(this.panels[prevPanel].id);
        }
    },
    selectPanel: function(panelId) {
        if (this.selected) {
            let tabpanel = document.querySelector("[id='" + this.selected + "']");
            if (tabpanel) {
                tabpanel.setAttribute("hidden", "");
            }
        }
        this.selected = panelId;
        let tabpanel = document.querySelector("[id='" + this.selected + "']");
        if (tabpanel) {
            tabpanel.removeAttribute("hidden");
        }
    },
    _label: function(panel) {
        if (panel.l10n) {
            return panel.l10n(panel.label);
        }
        if (this.l10n) {
            return this.l10n(panel.label);
        }
        return panel.label;
    },
    _setupTabpanel: function(panel) {
        let tabpanel = document.querySelector("[id='" + panel.id + "']");
        if (tabpanel == null) {
            return;
        }
        tabpanel.setAttribute("role", "tabpanel");
        tabpanel.setAttribute("aria-labelledby", 
            tabpanel.getAttribute('id') + '-tab');
        if (tabpanel.getAttribute('id') === this.selected) {
            tabpanel.removeAttribute("hidden");
        } else {
            tabpanel.setAttribute("hidden", "");
        }
    },
    _selectedPanel: function() {
        for (let i = 0; i < this.panels.length; i++) {
            let panel = this.panels[i];
            if (panel.id === this.selected) {
                return [panel, i];
            }
        }
        return [undefined, undefined];
    },
    _onKey: function(event) {
        if (event.type === "keydown") {
            if (this.isVertical 
                && [keys.up, keys.down].includes(event.keyCode)) {
                event.preventDefault();
            }
            return;
        }
        if (event.type !== "keyup") {
            return;
        }
        let [panel, panelIndex] = this._selectedPanel();
        if (!panel) {
            return;
        }
        let panels = this.panels;
        let handled = false;
        if (this.isVertical ? event.keyCode === keys.up
            : event.keyCode === keys.left) {
            this.selectPanel(panels[
                    (panelIndex-1+panels.length)%panels.length].id);
            handled = true;
        } else if (this.isVertical ? event.keyCode === keys.down
            : event.keyCode === keys.right) {
            this.selectPanel(panels[(panelIndex+1)%panels.length].id);
            handled = true;
        } else if (event.keyCode === keys.delete) {
            if (panel.removeCallback) {
                panel.removeCallback();
                handled = true;
            }
        } else if (event.keyCode === keys.home) {
            this.selectPanel(panels[0].id);
            handled = true;
        } else if (event.keyCode === keys.end) {
            this.selectPanel(panels[panels.length-1].id);
            handled = true;
        }
        if (handled) {
            event.preventDefault();
            let tab = document.querySelector(
                "[id='" + this.selected + "-tab'] > button");
            if (tab) {
                tab.focus();
            }
        }
    }
  },
  watch: {
    panels: function(newValue) {
        if (this.selected === null && newValue.length > 0) {
            this.selectPanel(this.panels[0].id);
        }
    },
  },
  template: `
    <div v-bind:id="id" class="jgwc-tablist" role="tablist"
      @keydown="_onKey($event)" @keyup="_onKey($event)">
      <span v-for="panel of panels" :id="panel.id + '-tab'" role="tab"
          :aria-selected="panel.id == selected ? 'true' : 'false'"
          :aria-controls="panel.id">
        <button type="button" :tabindex="panel.id == selected ? 0 : -1"
          v-on:click="selectPanel(panel.id)">{{ _label(panel) }}</button><button
        type="button" v-if="panel.removeCallback" class="fa fa-times"
          v-on:click="panel.removeCallback()" tabindex="-1"></button>
      </span>
    </div>
  `,
  mounted: function() {
    if (this.panels.length > 0) {
        this.selected = this.panels[0].id;
    }
    for (let panel of this.panels) {
        this._setupTabpanel(panel);
    }
  }
});

Vue.component('jgwc-modal-dialog', {
  props: {
    id: String,
    title: String,
    showCancel: {
        type: Boolean,
        default: true
    },
    content: String,
    contentClasses: Array,
    closeLabel: String,
    onClose: Function
  },
  data: function () {
    return {
      effectiveId: null,
      isOpen: false
    }
  },
  template: `
    <div class="jgwc-modal-dialog dialog__backdrop" :hidden="!isOpen">
      <div :id="effectiveId" role="dialog" :aria-labelledby="effectiveId + '-label'" 
        aria-modal="true">
        <header :id="effectiveId + '-label'">
          <p>{{ title }}</p>
          <button v-if="showCancel" type="button" class="fa fa-times" 
            v-on:click="cancel()"></button>
        </header>
        <section v-if="content" v-html="content" :class="contentClasses"></section>
        <section v-else :class="contentClasses"><slot></slot></section>
        <footer>
          <button type="button" v-on:click="close()">{{ closeLabel || "Okay" }}</button>
        </footer>
      </div>
    </div>
  `,
  methods: {
    open: function() {
        this.isOpen = true;
    },
    cancel: function() {
        if (this.onClose) {
            (this.onClose)(false);
        }
        this.isOpen = false;
    },
    close: function() {
        if (this.onClose) {
            (this.onClose)(true);
        }
        this.isOpen = false;
    }
  },
  created: function() {
    if (this.id) {
        this.effectiveId = id;
        return;
    }
    if (!this.constructor.prototype.$instanceCounter) {
        this.constructor.prototype.$instanceCounter = 0;
    }
    this.effectiveId = "jgwc-modal-dialog-" + ++this.constructor.prototype.$instanceCounter;
  }
});

var disclosures = Vue.observable({});

Vue.component('jgwc-disclosure-button', {
  props: {
    idRef: {
        type: String,
        required: true,
    },
    onShow: {
        type: Function,
        default: null,
    },
    onHide: {
        type: Function,
        default: null,
    },
    onToggle: {
        type: Function,
        default: null,
    },
  },
  data: function () {
    return {
        disclosed: false,
    }
  },
  methods: {
    toggleDisclosed: function() {
        this.disclosed = !this.disclosed;
        if (this.onToggle) {
            this.onToggle.call(this, this.disclosed);
        }
        if (this.disclosed) {
            if (this.onShow) {
                this.onShow.call(this);
            }
        } else {
            if (this.onHide) {
                this.onHide.call(this);
            }
        }
    }
  },
  template: `
    <button type="button" data-jgwc-role="disclosure-button"
      :aria-expanded="disclosed ? 'true' : 'false'"
      :aria-controls="idRef"
      @click="toggleDisclosed()"><slot></slot></button>
  `,
  created: function() {
      Vue.set(disclosures, this.idRef, this);
  },
  beforeDestroy: function() {
      Vue.delete(disclosures, this.idRef);
  },
});

var jgwcDisclosureButton = Vue.prototype.jgwc.disclosureButton = function(idRef) {
    // We have no control about when the component is created, so
    // it may not have been created yet. Vue's reactivity doesn't track
    // addition of properties, only changes to properties. So we
    // have to make sure that the property exists.
    if (disclosures[idRef] === undefined) {
        Vue.set(disclosures, idRef, null);
    }
    return disclosures[idRef];
}

Vue.prototype.jgwc.isDisclosed = function(idRef) {
    let button = jgwcDisclosureButton(idRef);
    return button ? button.disclosed : false;
}

Vue.component('jgwc-disclosure-section', {
  render: function(createElement) {
    let id = this.$attrs.id;
    let button = disclosures[id];
    if (button && button.disclosed) {
        let tag = this.$vnode.data.tag;
        return createElement(tag, this.$slots.default);
    }
    return null;
  },
});

export { jgwcIdScopeMixin };

/*
function vnodeAttr(vnode, attr) {
    let obj = vnode.data;
    if (!obj) {
        return undefined;
    }
    obj = obj.attrs;
    if (!obj) {
        return undefined;
    }
    obj = obj[attr];
    if (!obj) {
        return undefined;
    }
    return obj;
}

function processDisclosures(collected, vnode) {
    let role = vnodeAttr(vnode, "role");
    if (role && role.toLowerCase() === "button") {
        vnode.data.attrs.test="yes";
    }
    if (vnode.children) {
        for (let child of vnode.children) {
            processDisclosures(collected, child);
        }
    }
}

var disclosureMixin = {
    data: function() {
        return {
            disclosed: {}
        }
    },
    methods: {
        isDisclosed: function(key) {
            return (key in this.disclosed);
        },
        toggleDisclosure: function(key) {
            if (key in this.disclosed) {
                Vue.delete(this.disclosed, key);
                return;
            }
            Vue.set(this.disclosed, key, true);
        }
    },
    beforeMount: function() {
        let vm = this;
        let origRender = this.$options.render;
        this.$options.render = function() {
            let result = origRender.call(vm);
            processDisclosures({}, result);
            return result;
        }
    },
}

Vue.component('jgwc-aria', {
    template: '<td><slot></slot></td>',
    beforeMount: function() {
        let vm = this;
        let origRender = this.$options.render;
        this.$options.render = function() {
            let result = origRender.call(vm);
            return result;
        }
    }
});
*/
