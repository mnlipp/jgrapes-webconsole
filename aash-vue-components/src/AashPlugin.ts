/**
 * Provides the components from the library and a global context 
 * `$aash` that implements the interface `Aash`. 
 *
 * @module AashPlugin
 */

import { App, Ref, ref } from 'vue'
import { provideApi, getApi } from "./AashUtil";
import AashAccordionComponent from "./components/accordion/AashAccordion.vue";
import * as AashAccordion from "./components/accordion/AashAccordion";
import AashAccordionSectionComponent from "./components/accordion/AashAccordionSection.vue";
import * as AashAccordionSection from "./components/accordion/AashAccordionSection";
import AashDisclosureButtonComponent from "./components/disclosure/AashDisclosureButton.vue";
import * as AashDisclosureButton from "./components/disclosure/AashDisclosureButton";
import AashDropdownMenuComponent from "./components/dropdown-menu/AashDropdownMenu.vue";
import * as AashDropdownMenu from "./components/dropdown-menu/AashDropdownMenu";
import AashModalDialogComponent from "./components/modal-dialog/AashModalDialog.vue";
import * as AashModalDialog from "./components/modal-dialog/AashModalDialog";
import AashTablistComponent from "./components/tablist/AashTablist.vue";
import * as AashTablist from "./components/tablist/AashTablist";
import AashTreeViewComponent from "./components/tree-view/AashTreeView.vue";
import * as AashTreeView from "./components/tree-view/AashTreeView";

export { provideApi, getApi,
    AashDisclosureButtonComponent, AashDisclosureButton,
    AashDropdownMenuComponent, AashDropdownMenu, 
    AashTablistComponent, AashTablist, 
    AashModalDialogComponent, AashModalDialog,
    AashTreeViewComponent, AashTreeView
};

let disclosureRegistry = new Map();
let idCounter = 0;
    

/**
 * The context provided by the Aash library as global context `$aash`.
 * @memberOf module:AashPlugin
 */
interface Aash {
    /**
     * Generates a unique id.
     * @function
     * @returns the id
     */
    generateId(): string;
    
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
    
    generateId() {
        return "aash-id-" + ++idCounter;
    },
    
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
    install: (app: App) => {
        app.config.globalProperties.$aash = aash;
        app.component('aash-disclosure-button', AashDisclosureButtonComponent);
        app.component('aash-dropdown-menu', AashDropdownMenuComponent);
        app.component('aash-modal-dialog', AashModalDialogComponent);
        app.component('aash-tablist', AashTablistComponent);
        app.component('aash-tree-view', AashTreeViewComponent);
        app.component('aash-accordion', AashAccordionComponent);
        app.component('aash-accordion-section', AashAccordionSectionComponent);
  }
}