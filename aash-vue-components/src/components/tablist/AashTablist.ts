import Vue from "vue";
import { Component, Prop, Watch } from "vue-property-decorator";

type Panel = {
  id: string;
  removeCallback: () => {}
};

@Component
/**
 * Generates a tab list with all required ARIA attributes.
 * 
 * Panels are described by objects with these properties:
 * * *id* (`string`): the id of the HTML element that is enabled or disabled
 *   depending on the selected tab.
 * * *label* (`string`|`function`): a string used as label for the tab
 *   or a function that returns the label
 * * *removeCallback* (`function`): Called when the tab is removed.
 *
 * Once created, a component provides some externally invocable methods:
 * * *addPanel(panel)*: adds another panel.
 * * *removePanel(panelId)*: removes the panel with the given id.
 * * *selectPanel(panelId)*: activates the panel with the given id.
 *
 * @function
 * @param {Object} props the properties
 * @param {string} props.id the id for the enclosing `div`
 * @param {Array} props.initialPanels the list of initial panels
 * @param {function} props.l10n a function invoked with a label 
 *      (of type string) as argument before the label is rendered
 */
export default class AashTablist extends Vue {
    // The id attribute of the generated top level HTML element 
    @Prop({ type: String, required: true }) readonly id!: string;
    // Initial panels 
    @Prop({ type: Array }) initialPanels!: Array<Panel>;
    // A function invoked with a label 
    // (of type string)as argument before the label is rendered
    @Prop({ type: Function }) l10n: any;

    panels: Array<Panel> = this.initialPanels || [];
    selected: any = null;

    get isVertical() {
        return this.$attrs["aria-orientation"] !== undefined
            && this.$attrs["aria-orientation"] === "vertical";        
    }

    addPanel(panel: Panel) {
        this.panels.push(panel);
        this._setupTabpanel(panel)
    }
    
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

    private _label(panel: any) {
        if (typeof panel.label === 'function') {
            return panel.label();
        }
        if (this.l10n) {
            return this.l10n(panel.label);
        }
        return panel.label;
    }

    private _setupTabpanel(panel: any) {
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

