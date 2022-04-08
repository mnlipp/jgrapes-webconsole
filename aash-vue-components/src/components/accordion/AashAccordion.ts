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
    private _singleExpansion = true;
    private _domRoot: HTMLElement | null = null;
    private _sections: string[] = [];
    private _expanded = new Set();
    
    constructor(singleExpansion: boolean) {
        this._singleExpansion = singleExpansion;
    }

    setDomRoot(root: HTMLElement) {
        this._domRoot = root;
    }
    
    addSection(id: string): number {
        this._sections.push(id);
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
 * <aash-accordion>
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
 * Resulting HTML:
 * ```html
 * <div data-aash-role="accordion">
 *   <div role="header">
 *     <button id="aash-id-6-control" aria-controls="aash-id-6-panel" 
 *       aria-expanded="true"><span>Header 1</span></button>
 *   </div>
 *   <div id="aash-id-6-panel" role="region" aria-labelledby="aash-id-6-control">
 *     Panel 1 
 *   </div>
 *   <div role="header">
 *     <button id="aash-id-7-control" aria-controls="aash-id-7-panel" 
 *       aria-expanded="false">Header 2</button>
 *   </div>
 *   <div id="aash-id-7-panel" role="region" aria-labelledby="aash-id-7-control"
 *     hidden="">
 *     Panel 2
 *   </div>
 * </div>
 * ```
 * 
 * @class AashAccordionComponent
 * @param {Object} props the properties
 * @param {Boolean} props.singleExpansion whether only one section
 * may be expanded at a given point in time, defaults to `true`
 * @param {string} props.headerType the element type for the headers,
 * defaults to "div"
 * @param {string} props.buttonType the element type for the buttons,
 * defaults to "button"
 * @param {string} props.panelType the element type for the panels,
 * defaults to "div"
 */
export default defineComponent({
    props: {
        singleExpansion: { type: Boolean, default: true },
        headerType: { type: String, default: "div" },
        buttonType: { type: String, default: "button" },
        panelType: { type: String, default: "div" }
    },
    
    setup(props) {
        provide('headerType', props.headerType);
        provide('buttonType', props.buttonType);
        provide('panelType', props.panelType);
        
        let controller = reactive(new Controller(props.singleExpansion));
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
