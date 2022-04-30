/**
 * Provides an element that controls the visibility of 
 * vertically stacked sections.
 *
 * @module AashAccordion
 */
import { defineComponent, getCurrentInstance, onUnmounted, provide, reactive, 
    ref, nextTick, onMounted } from 'vue'
import { provideApi } from "../../AashUtil";

/**
 * The interface provided by the component.
 *
 * * `expand(index: number): void`: ensures that the section is expanded 
 *
 * @memberof module:AashAccordion
 */
export interface Api {
  expand(index: number): void;
}

export class Controller {
    private _alwaysExpanded = false;
    private _singleExpansion = true;
    private _domRoot: HTMLElement | null = null;
    private _sections: string[] = [];
    private _expanded = new Set();
    
    constructor(alwaysExpanded: boolean, singleExpansion: boolean) {
        this._alwaysExpanded = alwaysExpanded;
        this._singleExpansion = singleExpansion;
    }

    setDomRoot(root: HTMLElement) {
        this._domRoot = root;
    }
    
    addSection(id: string): number {
        this._sections.push(id);
        if (this._alwaysExpanded && this._expanded.size == 0) {
            this._expanded.add(0);
        }
        return this._sections.length - 1;
    }
    
    isExpanded(index: number): boolean {
        return this._expanded.has(index);
    }
    
    expand(index: number) {
        if (this._expanded.has(index)) {
            return;
        }
        this.toggleExpanded(index);
    }
    
    toggleExpanded(index: number) {
        if (this._expanded.has(index)) {
            if (this._alwaysExpanded && this._expanded.size == 1) {
                return;
            }
            this._expanded.delete(index);
            return;
        }
        if (this._singleExpansion) {
            this._expanded.clear();
        }
        this._expanded.add(index);
    }
    
    selectNext(index: number) {
        if (index >= this._sections.length) {
            return;
        }
        this._updateDomFocus(index + 1);
    }
    
    selectPrev(index: number) {
        if (index == 0) {
            return;
        }
        this._updateDomFocus(index - 1);
    }
    
    selectFirst() {
        this._updateDomFocus(0);
    }
    
    selectLast() {
        this._updateDomFocus(this._sections.length - 1);
    }
    
    private _updateDomFocus(index: number) {
        nextTick (() => {
            let cur = this._domRoot?.querySelector(
                "#" + this._sections[index] + "-control");
            (<HTMLElement>cur)?.focus();
        });
    }
}

/**
 * @classdesc
 * Generates an accordion.
 * 
 * Sections are added using `aash-accordion-section`.
 *
 * Example source:
 * ```html
 * <aash-accordion :always-expanded="true">
 *   <aash-accordion-section :title="'Header 1'">
 *     Panel 1
 *   </aash-accordion-section>
 *   <aash-accordion-section>
 *     <template #title>Header 2</template>
 *       Panel 2
 *   </aash-accordion-section>
 * </aash-accordion>
 * ```
 *
 * The resulting DOM follows the example shown in the 
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/accordion/accordion.html).
 * Notable differences are a `div` surrounding the pairs of headers and panels
 * and an additional div between the panel and its content.
 *
 * The former simplifies styling of a section independant of its expanded state
 * with e.g. a shadow.
 *
 * The latter is required for animating accordions. Details can be found in
 * the documentation od `AashAccordionSection`.
 *
 * ```html
 * <div data-aash-role="accordion">
 *   <div>
 *     <div role="header">
 *       <button id="aash-id-6-control" tabindex="0" 
 *         aria-controls="aash-id-6-panel" aria-expanded="true">
 *         <span>Header 1</span>
 *       </button>
 *     </div>
 *     <div id="aash-id-6-panel" role="region" class="" 
 *       aria-labelledby="aash-id-6-control">
 *       <div> Panel 1 </div>
 *     </div>
 *   </div>
 *   <div>
 *     <div role="header">
 *       <button id="aash-id-7-control" tabindex="0" 
 *         aria-controls="aash-id-7-panel" aria-expanded="false">Header 2</button>
 *     </div>
 *     <div id="aash-id-7-panel" role="region" class="" 
 *       aria-labelledby="aash-id-7-control" hidden="">
 *       <div> Panel 2 </div>
 *     </div>
 *   </div>
 * </div>
 * ```
 * 
 * @class AashAccordionComponent
 * @param {Object} props the properties
 * @param {Boolean} props.alwaysExpanded make sure that at least,
 * one panel is always expanded, defaults to `false`
 * @param {Boolean} props.singleExpansion whether only one section
 * may be expanded at a given point in time, defaults to `true`
 * @param {string} props.headerType the element type for the headers,
 * defaults to "div"
 * @param {string} props.buttonType the element type for the buttons,
 * defaults to "button"
 * @param {string} props.panelType the element type for the panels,
 * defaults to "div"
 * @param {string} props.panelClass the class attribute for the panels,
 * defaults to `null`
 */
export default defineComponent({
    props: {
        alwaysExpanded: { type: Boolean, default: false },
        singleExpansion: { type: Boolean, default: true },
        headerType: { type: String, default: "div" },
        buttonType: { type: String, default: "button" },
        panelType: { type: String, default: "div" },
        panelClass: { type: String, default: null }
    },
    
    setup(props) {
        provide('headerType', props.headerType);
        provide('buttonType', props.buttonType);
        provide('panelType', props.panelType);
        provide('panelClass', props.panelClass);
        
        let controller = reactive(new Controller(
            props.alwaysExpanded, props.singleExpansion));
        provide('controller', controller);
        
        const domRoot = ref(null);

        onMounted(() => {
            controller.setDomRoot(domRoot.value!);
        });

        provideApi(domRoot, {
            expand: (index: number) => {
                controller.expand(index);
            }
        });
        
        return { domRoot };
    }
});
