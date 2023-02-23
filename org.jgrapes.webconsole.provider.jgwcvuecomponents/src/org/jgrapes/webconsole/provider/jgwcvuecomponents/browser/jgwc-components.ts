/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2019, 2022  Michael N. Lipp
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

import { ref, App, Plugin } from "vue"
import AashPlugin, { AashDropdownMenu, AashTablist, AashModalDialog, 
    AashDisclosureButton } from "aash-plugin"
import JGConsole from "jgconsole"

var scopeCounter = 1;

class IdScope {

    private _idScope: number;
    
    constructor() {
        this._idScope = scopeCounter++;
    }

    /**
     * The function that returns the prefixed id.
     *
     * @param {string} id the id to prefix
     */
    scopedId(id: string) {
        return "jgwc-id-" + this._idScope + "-" + id;
    }    
}

/*
 * Install a MutationObserver for root html node's attribute lang
 * that feeds back the value to a the lang attribute of the
 * Vue observable Vue.prototype.jgwc.observed.
 */
let htmlRoot = document.querySelector("html")!;
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
    static unmountVueApps(content: HTMLElement) {
        if ("__vue_app__" in <any>content) {
            (<any>content).__vue_app__.unmount();
            return;
        }
        for (let child = 0; child < content.children.length; child++) {
            JGWC.unmountVueApps(<HTMLElement>content.children.item(child)!);
        }
    }
    
    /**
     * Selected language as reactive property.
     */
    static lang() {
        return langRef.value;
    }

    /**
     * Return a new id scope
     * @return {IdScope}
     */    
    static createIdScope() {
        return new IdScope();
    }
}

// For access from plain JavaScript.
(<any>JGConsole).jgwc = JGWC;

export const JgwcPlugin: Plugin = {
    install: (app: App, _: any) => {
        app.config.globalProperties.$jgwc = JGWC;
        app.config.globalProperties.JGConsole = JGConsole;
        app.use(AashPlugin);
        app.component('jgwc-dropdown-menu', AashDropdownMenu);
        app.component('jgwc-tablist', AashTablist);
        app.component('jgwc-modal-dialog', AashModalDialog);
        app.component('jgwc-disclosure-button', AashDisclosureButton);
  }
}

export default JgwcPlugin;

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
