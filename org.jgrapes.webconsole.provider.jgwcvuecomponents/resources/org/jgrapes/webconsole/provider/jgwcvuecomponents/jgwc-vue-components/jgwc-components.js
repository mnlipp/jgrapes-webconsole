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

/**
 * Importing this module 
 *  * adds class {@link JGWC} to the global
 *    `JGConsole` and thus makes its static members easily accessible
 *    from non module contexts,
 *  * adds an object to `Vue.prototype` as attribute `jgwc`,
 *  * provides a Vue observable `Vue.prototype.jgwc.observed` with
 *    attribute `lang` that changes whenever the attribute `lang`
 *    of the root `html` node in the DOM changes,
 *  * provides some Vue mixins,
 *  * registers some Vue components 
 *
 * @module jgwc-vue-components/jgwc-components
 */

import Vue from "../vue/vue.esm.browser.js"
import { AashDropdownMenu, AashTablist, AashModalDialog } 
    from "../aash-vue-components/lib/aash-vue-components.js"
import JGConsole from "../../console-base-resource/jgconsole.js"

/**
 * Used as scope, static members only.
 *
 * The class is also published as `JGConsole.jgwc`.
 */
export class JGWC {
    /**
     * Destroy all view models in the given subtree.
     *
     * @param {HTMLElement} content the root of the subtree to be search for
     *      vue vms
     */
    static destroyVMs(content) {
        if ("__vue__" in content) {
            content.__vue__.$destroy();
            return;
        }
        for (let child of content.children) {
            JGConsole.jgwc.destroyVMs(child);
        }
    }
}

JGConsole.jgwc = JGWC;

Vue.jgwc = {};
Vue.prototype.jgwc = {};

/*
 * Install a MutationObserver for root html node's attribute lang
 * that feeds back the value to a the lang attribute of the
 * Vue observable Vue.prototype.jgwc.observed.
 */
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

/**
 * A Vue mixin that provides the component with a unique scope for ids.
 *
 * @mixin
 */
export var jgwcIdScopeMixin = {
    created: function() {
        this._idScope = scopeCounter++;
    },
    methods: {
        /**
         * The mixed in function that returns the prefixed id.
         *
         * @function scopedId
         * @memberof jgwcIdScopeMixin
         * @instance
         * @param {string} id the id to prefix
         * @return {string}
         */
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

Vue.component('jgwc-dropdown-menu', AashDropdownMenu);
Vue.component('jgwc-tablist', AashTablist);
Vue.component('jgwc-modal-dialog', AashModalDialog);

var disclosures = Vue.observable({});

Vue.component('jgwc-disclosure-button', {
  props: {
    idRef: {
        type: String,
        required: true,
    },
    type: {
        type: String,
        default: "button",
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
    <component :is="type"
      v-bind="type === 'button' ? { type: 'button'}
        : { 'aria-role': 'button', tabindex: '0' }" 
      data-jgwc-role="disclosure-button"
      :aria-expanded="disclosed ? 'true' : 'false'"
      :aria-controls="idRef"
      @click="toggleDisclosed()"><slot></slot></component>
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
