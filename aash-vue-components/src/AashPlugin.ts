/**
 * Provides the components from the library and a global context 
 * `$aash` of type {@link Aash}
 *
 * @module AashPlugin
 */

import { App, Ref, ref } from 'vue'
import AashDropdownMenu from "./components/dropdown-menu/AashDropdownMenu.vue";
import AashTablist from "./components/tablist/AashTablist.vue";
import AashModalDialog from "./components/modal-dialog/AashModalDialog.vue";
import AashDisclosureButton from "./components/disclosure/AashDisclosureButton.vue";

export { AashDropdownMenu, AashTablist, AashModalDialog, 
    AashDisclosureButton };

let disclosureRegistry = new Map();

/**
 * The context provided by the Aash library as global context `$aash`.
 * @memberOf module:AashPlugin
 */
interface Aash {
    /**
     * Returns the `Vuew.Ref` the represents the state of the disclosure
     * with the given `id`.
     * @function
     * @param id the id
     */
    disclosureData(id: string): Ref<boolean>;
    
    /**
     * Returns the state of the disclosure
     * with the given `id`.
     * @function
     * @param id the id
     */
    isDisclosed(id: string): boolean;
}

let aash: Aash = {
    disclosureData: (id) => {
        if (disclosureRegistry.has(id)) {
            return disclosureRegistry.get(id);
        }
        let disclosed = ref(false);
        disclosureRegistry.set(id, disclosed);
        return disclosed;
    },
    
    isDisclosed: (id) => {
        return aash.disclosureData(id).value;
    }
}

export default {
    install: (app: App, options?: {}) => {
        app.config.globalProperties.$aash = aash;
        app.component('aash-dropdown-menu', AashDropdownMenu);
        app.component('aash-tablist', AashTablist);
        app.component('aash-modal-dialog', AashModalDialog);
        app.component('aash-disclosure-button', AashDisclosureButton);
  }
}