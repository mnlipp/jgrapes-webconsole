/**
 * Provides the components from the library and a global context 
 * `$aash` of type {@link Aash}
 *
 * @module AashPlugin
 */

import { App, Ref, ref } from 'vue'
import { provideApi, getApi } from "./AashUtil";
import AashDisclosureButtonComponent, * as AashDisclosureButton from "./components/disclosure/AashDisclosureButton.vue";
import AashDropdownMenuComponent, * as AashDropdownMenu from "./components/dropdown-menu/AashDropdownMenu.vue";
import AashModalDialogComponent, * as AashModalDialog from "./components/modal-dialog/AashModalDialog.vue";
import AashTablistComponent, * as AashTablist from "./components/tablist/AashTablist.vue";
import AashTreeViewComponent, * as AashTreeView from "./components/tree-view/AashTreeView.vue";

export { provideApi, getApi,
    AashDisclosureButtonComponent, AashDisclosureButton,
    AashDropdownMenuComponent, AashDropdownMenu, 
    AashTablistComponent, AashTablist, 
    AashModalDialogComponent, AashModalDialog,
    AashTreeViewComponent, AashTreeView
};

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
     * Remove the discloure data for the given `id`.
     * @function
     * @param id the id
     */
    removeDisclosure(id: string): void;
    
    /**
     * Returns the state of the disclosure
     * with the given `id`.
     * @function
     * @param id the id
     */
    isDisclosed(id: string): boolean;
}

let aash: Aash = {
    disclosureData(id) {
        if (disclosureRegistry.has(id)) {
            return disclosureRegistry.get(id);
        }
        let disclosed = ref(false);
        disclosureRegistry.set(id, disclosed);
        return disclosed;
    },
    
    isDisclosed(id) {
        return aash.disclosureData(id).value;
    },
    
    removeDisclosure(id) {
        return disclosureRegistry.delete(id);
    }
}

export default {
    install: (app: App, options?: {}) => {
        app.config.globalProperties.$aash = aash;
        app.component('aash-disclosure-button', AashDisclosureButtonComponent);
        app.component('aash-dropdown-menu', AashDropdownMenuComponent);
        app.component('aash-modal-dialog', AashModalDialogComponent);
        app.component('aash-tablist', AashTablistComponent);
        app.component('aash-tree-view', AashTreeViewComponent);
  }
}