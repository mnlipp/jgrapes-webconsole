/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2019, 2021  Michael N. Lipp
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

import { ref } from "../vue/vue.esm-browser.js"
import AashPlugin, { 
    AashDropdownMenu, AashTablist, AashModalDialog, AashDisclosureButton } 
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

/*
 * Install a MutationObserver for root html node's attribute lang
 * that feeds back the value to a the lang attribute of the
 * Vue observable Vue.prototype.jgwc.observed.
 */
let htmlRoot = document.querySelector("html");
let langRef = ref(htmlRoot.getAttribute('lang'));

new MutationObserver(function(mutations) {
    mutations.forEach((mutation) => {
        if (mutation.type === 'attributes'
            && mutation.attributeName === 'lang') {
            langRef.value = htmlRoot.getAttribute('lang');
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

export default {
    install: (app, options) => {
        app.config.globalProperties.$jgwc = {
            lang: () => { return langRef.value }
        };
        app.use(AashPlugin);
        app.component('jgwc-dropdown-menu', AashDropdownMenu);
        app.component('jgwc-tablist', AashTablist);
        app.component('jgwc-modal-dialog', AashModalDialog);
        app.component('jgwc-disclosure-button', AashDisclosureButton);
  }
}

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
