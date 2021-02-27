import Vue from "vue";
import { Component, Prop, Watch } from "vue-property-decorator";

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
 * Once created, a component provides the externally invocable methods
 * described below.
 */
@Component
export default class AashTablist extends Vue {
    /** The id attribute of the generated top level HTML element
     * @category Vue Component */ 
    @Prop({ type: String, required: true }) readonly id!: string;
    /** The initial panels
     * @category Vue Component */  
    @Prop({ type: Array }) initialPanels!: Array<Panel>;
    /** A function invoked with a label as argument before the label is rendered
     * @category Vue Component */  
    @Prop({ type: Function }) l10n: ((label: string) => string) | undefined;

    panels: Array<Panel> = this.initialPanels || [];
    selected: any = null;

    get isVertical() {
        return this.$attrs["aria-orientation"] !== undefined
            && this.$attrs["aria-orientation"] === "vertical";        
    }

    /**
     * Adds another panel.
     * @param panel the panel to add
     */
    addPanel(panel: Panel) {
        this.panels.push(panel);
        this._setupTabpanel(panel)
    }

    /**
     * Removes the panel with the given id.
     * @param panelId the panelId
     */
    removePanel(panelId: string) {
        let prevPanel = 0;
        for (let i = 0; i < this.panels.length; i++) {
            if (this.panels[i].id === panelId) {
                this.panels.splice(i, 1);
                break;
            }
            prevPanel = i;
        }
        if (this.panels.length > 0) {
            this.selectPanel(this.panels[prevPanel].id);
        }
    }

    /**
     * Activates the panel with the given id.
     * @param panelId the panelId
     */
    selectPanel(panelId: string) {
        if (this.selected) {
            let tabpanel = document.querySelector("[id='" + this.selected + "']");
            if (tabpanel) {
                tabpanel.setAttribute("hidden", "");
            }
        }
        this.selected = panelId;
        let tabpanel = document.querySelector("[id='" + this.selected + "']");
        if (tabpanel) {
            tabpanel.removeAttribute("hidden");
        }
    }

    private _label(panel: Panel) {
        if (typeof panel.label === 'function') {
            return panel.label();
        }
        if (this.l10n) {
            return this.l10n(panel.label);
        }
        return panel.label;
    }

    private _setupTabpanel(panel: Panel) {
        let tabpanel = document.querySelector("[id='" + panel.id + "']");
        if (tabpanel == null) {
            return;
        }
        tabpanel.setAttribute("role", "tabpanel");
        tabpanel.setAttribute("aria-labelledby", 
            tabpanel.getAttribute('id') + '-tab');
        if (tabpanel.getAttribute('id') === this.selected) {
            tabpanel.removeAttribute("hidden");
        } else {
            tabpanel.setAttribute("hidden", "");
        }
    }
    
    private _selectedPanel(): [Panel | null, number] {
        for (let i = 0; i < this.panels.length; i++) {
            let panel = this.panels[i];
            if (panel.id === this.selected) {
                return [panel, i];
            }
        }
        return [null, -1];
    }

    private _onKey(event: KeyboardEvent) {
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
                "[id='" + this.selected + "-tab'] > button");
            tab?.focus();
        }
    }
    
    @Watch('panels')
    onPanelsChange(newValue: any) {
        if (this.selected === null && newValue.length > 0) {
            this.selectPanel(this.panels[0].id);
        }
    }
    
    mounted() {
      if (this.panels.length > 0) {
          this.selected = this.panels[0].id;
      }
      for (let panel of this.panels) {
          this._setupTabpanel(panel);
      }
  }
}

