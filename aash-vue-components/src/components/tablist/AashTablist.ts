/**
 * @module AashTablist
 */
import { defineComponent, PropType, ref, computed } from 'vue'

/**
 * The information about a panel managed by the tablist. 
 */
export type Panel = {
  /** The id of the panel's root node */
  id: string;
  /** The label to use for the panel */
  label: string | Function;
  /** A function to call when the panel is removed (optional) */
  removeCallback: () => {}
};

/**
 * @classdesc
 * Generates a 
 * [tab list element](https://www.w3.org/TR/wai-aria-practices-1.1/#tabpanel) 
 * and its child tab elements with all required ARIA attributes. 
 * All tab elements have an `aria-controls` attribute that references the 
 * associated tab panel. 
 * 
 * The tab panels controlled by the tab list are made known by objects of 
 * type {@link Panel}. Because the tab panels are referenced from the 
 * tab elements, the tab panel elements need only
 * an `id` attribute and `role=tabpanel` `tabindex=0`.
 *
 * Once created, a component provides some externally invocable methods:
 * * *addPanel(panel: Panel)*: adds another panel.
 * * *removePanel(panelId: string)*: removes the panel with the given id.
 * * *selectPanel(panelId: string)*: activates the panel with the given id.
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
        initialPanels: Array as PropType<Array<Panel>>,
        l10n: Function
    },

    setup(props) {
        const panels = ref(props.initialPanels || []); 
        const selected: any = ref(null);
        
        const addPanel = (panel: Panel) => {
            panels.value.push(panel);
            _setupTabpanel(panel)
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

        const _label = (panel: Panel) => {
            if (typeof panel.label === 'function') {
                return panel.label();
            }
            if (props.l10n) {
                return props.l10n(panel.label);
            }
            return panel.label;
        };

        const _setupTabpanel = (panel: Panel) => {
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

        const _selectedPanel = function(): [Panel | null, number] {
            for (let i = 0; i < panels.value.length; i++) {
                let panel = panels.value[i];
                if (panel.id === selected.value) {
                    return [panel, i];
                }
            }
            return [null, -1];
        }

        let id = props.id;

        return { id, panels, selected, addPanel, removePanel, 
            selectPanel, _selectedPanel, _setupTabpanel }; 
    },

    methods: {
        _onKey(event: KeyboardEvent) {
            if (event.type === "keydown") {
                if (this.isVertical 
                    && ["ArrowUp", "ArrowDown"].includes(event.key)) {
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
            if (this.isVertical ? event.key === "ArrowUp"
                : event.key === "ArrowLeft") {
                this.selectPanel(panels[
                        (panelIndex-1+panels.length)%panels.length].id);
                handled = true;
            } else if (this.isVertical ? event.key === "ArrowDown"
                : event.key === "ArrowRight") {
                this.selectPanel(panels[(panelIndex+1)%panels.length].id);
                handled = true;
            } else if (event.key === "Delete") {
                if (panel.removeCallback) {
                    panel.removeCallback();
                    handled = true;
                }
            } else if (event.key === "Home") {
                this.selectPanel(panels[0].id);
                handled = true;
            } else if (event.key === "End") {
                this.selectPanel(panels[panels.length-1].id);
                handled = true;
            }
            if (handled) {
                event.preventDefault();
                let tab: HTMLElement | null = document.querySelector(
                    "[id='" + this.selected.value + "-tab'] > button");
                tab?.focus();
            }
        }
    },
    
    computed: {
        isVertical(): boolean {
            return this.$attrs["aria-orientation"] !== undefined
                && this.$attrs["aria-orientation"] === "vertical";
        }
    },
    
    watch: {
        panels(newValue) {
            if (this.selected === null && newValue.length > 0) {
                this.selectPanel(this.panels[0].id);
            }
        }
    },
    
    mounted() {
        if (this.panels.length > 0) {
            this.selected = this.panels[0].id;
        }
        for (let panel of this.panels) {
            this._setupTabpanel(panel);
        }
    }
});
