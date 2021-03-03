/**
 * @module AashTablist
 */
import { defineComponent, PropType, ref, computed, onMounted, watch } from 'vue'

/**
 * The information about a panel managed by the tablist. 
 * @memberOf module:AashTablist
 */
export type Panel = {
  /** The id of the panel's root node */
  id: string;
  /** The label to use for the panel */
  label: string | Function;
  /** A function to call when the panel is removed (optional) */
  removeCallback?: () => {}
};

/**
 * The interface provided by the component.
 *
 * * `addPanel(panel: Panel): void`: adds another panel.
 * * `removePanel(panelId: string): void`: removes the panel with the given id.
 * * `selectPanel(panelId: string): void`: activates the panel with the given id.
 *
 * @memberOf module:AashTablist
 */
export interface AashApi {
  addPanel(panel: Panel): void;
  removePanel(panelId: string): void;
  selectPanel(panelId: string): void;
}

/**
 * @classdesc
 * Generates a 
 * [tab list element](https://www.w3.org/TR/wai-aria-practices-1.1/#tabpanel) 
 * and its child tab elements with all required ARIA attributes. 
 * All tab elements have an `aria-controls` attribute that references the 
 * associated tab panel. 
 * 
 * The tab panels controlled by the tab list are made known by objects of 
 * type {@link module:AashTablist.Panel Panel}. Because the tab panels are 
 * referenced from the 
 * tab elements, the tab panel elements need only
 * an `id` attribute and `role=tabpanel` `tabindex=0`.
 *
 * Once created, a component provides the externally invocable methods
 * defined by {@link module:AashTablist.API} through an object in 
 * the property `__aashApi` of the mounted DOM element.
 *
 * The DOM is generated as shown in the 
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/tabs/tabs-2/tabs.html)
 *
 * Example:
 * ```html
 * <div>
 *  <div id="sampleTabs" class="aash-tablist" role="tablist">
 *   <span id="tab-1-tab" role="tab" aria-selected="true"
 *    aria-controls="tab-1">
 *    <button type="button" tabindex="0">Tab 1</button>
 *   </span>
 *   <span id="tab-2-tab" role="tab" aria-selected="false"
 *    aria-controls="tab-2">
 *     <button type="button" tabindex="-1">Tab 2</button>
 *   </span>
 *  </div>
 * </div>
 * <div id="tab-1" role="tabpanel" aria-labelledby="tab-1-tab">This
 *  is panel One.</div>
 * <div id="tab-2" role="tabpanel" aria-labelledby="tab-2-tab" hidden="">
 *  This is panel Two.</div>
 * ```
 * 
 * @class AashTablist
 * @param {Object} props the properties
 * @param {string} props.id the id for the enclosing `div`
 * @param {Panel[]} props.initialPanels the list of initial panels
 * @param {function} props.l10n a function invoked with a label 
 *      (of type string) as argument before the label is rendered
 */
export default defineComponent({
    props: {
        id: { type: String, required: true },
        initialPanels: { type: Array as PropType<Array<Panel>> },
        l10n: { type: Function as PropType<((key: string) => string)> }
    },

    setup(props, context) {
        const panels = ref(props.initialPanels || []); 
        const selected: any = ref(null);
        
        const isVertical = computed(() => {
            return context.attrs["aria-orientation"] !== undefined
                && context.attrs["aria-orientation"] === "vertical";
        });
        
        const addPanel = (panel: Panel) => {
            panels.value.push(panel);
            setupTabpanel(panel)
        };

        const removePanel = (panelId: string) => {
            let prevPanel = 0;
            for (let i = 0; i < panels.value.length; i++) {
                if (panels.value[i].id === panelId) {
                    panels.value.splice(i, 1);
                    break;
                }
                prevPanel = i;
            }
            if (panels.value.length > 0) {
                selectPanel(panels.value[prevPanel].id);
            }
        };

        const selectPanel = (panelId: string) => {
            if (selected.value) {
                let tabpanel = document.querySelector("[id='" + selected.value + "']");
                if (tabpanel) {
                    tabpanel.setAttribute("hidden", "");
                }
            }
            selected.value = panelId;
            let tabpanel = document.querySelector("[id='" + selected.value + "']");
            if (tabpanel) {
                tabpanel.removeAttribute("hidden");
            }
        };

        const label = (panel: Panel) => {
            if (typeof panel.label === 'function') {
                return panel.label();
            }
            if (props.l10n) {
                return props.l10n(panel.label);
            }
            return panel.label;
        };

        const setupTabpanel = (panel: Panel) => {
            let tabpanel: HTMLElement | null = document.querySelector(
                "[id='" + panel.id + "']");
            if (tabpanel == null) {
                return;
            }
            tabpanel.setAttribute("role", "tabpanel");
            tabpanel.setAttribute("aria-labelledby", 
                tabpanel.getAttribute('id') + '-tab');
            if (tabpanel.getAttribute('id') === selected.value) {
                tabpanel.removeAttribute("hidden");
            } else {
                tabpanel.setAttribute("hidden", "");
            }
        };

        const selectedPanel = function(): [Panel | null, number] {
            for (let i = 0; i < panels.value.length; i++) {
                let panel = panels.value[i];
                if (panel.id === selected.value) {
                    return [panel, i];
                }
            }
            return [null, -1];
        }

        const onKey = (event: KeyboardEvent) => {
            if (event.type === "keydown") {
                if (isVertical.value 
                    && ["ArrowUp", "ArrowDown"].includes(event.key)) {
                    event.preventDefault();
                }
                return;
            }
            if (event.type !== "keyup") {
                return;
            }
            let [panel, panelIndex] = selectedPanel();
            if (!panel) {
                return;
            }
            let handled = false;
            if (isVertical.value ? event.key === "ArrowUp"
                : event.key === "ArrowLeft") {
                selectPanel(panels.value[
                        (panelIndex-1+panels.value.length)%panels.value.length].id);
                handled = true;
            } else if (isVertical.value ? event.key === "ArrowDown"
                : event.key === "ArrowRight") {
                selectPanel(panels.value[(panelIndex+1)%panels.value.length].id);
                handled = true;
            } else if (event.key === "Delete") {
                if (panel.removeCallback) {
                    panel.removeCallback();
                    handled = true;
                }
            } else if (event.key === "Home") {
                selectPanel(panels.value[0].id);
                handled = true;
            } else if (event.key === "End") {
                selectPanel(panels.value[panels.value.length-1].id);
                handled = true;
            }
            if (handled) {
                event.preventDefault();
                let tab: HTMLElement | null = document.querySelector(
                    "[id='" + selected.value + "-tab'] > button");
                tab?.focus();
            }
        }

        const tablist = ref(null);

        onMounted(() => {
            let api: AashApi = { addPanel, removePanel, selectPanel };
            if (tablist.value) {
                (<any>(tablist.value!)).__aashApi = api;
            }
            if (panels.value.length > 0) {
                selected.value = panels.value[0].id;
            }
            for (let panel of panels.value) {
                setupTabpanel(panel);
            }
        });

        watch(panels, (oldValue, newValue) => {
            if (selected.value === null && newValue.length > 0) {
                selectPanel(panels.value[0].id);
            }
        });
    
        return { panels, selected, label, tablist, onKey, selectPanel }; 
    }

});
