/**
 * Provides tablist element.
 * @module AashTablist
 */
import Alpine from 'alpinejs';

/**
 * The information about a panel managed by the tablist.
 */
export type Panel = {
  /** The id of the panel's root node */
  id: string;
  /** The label to use for the panel */
  label: string | (() => string);
  /** A function to call when the panel is removed (optional) */
  removeCallback?: () => void;
};

/**
 * @internal AlpineJS data definition for the tablist component.
 */
interface TablistData {
  panels: Panel[];
  selected: string | null;
  isVertical: boolean;
  l10n: ((key: string) => string) | null;
  selectPanel: (panelId: string) => void;
  label: (panel: Panel) => string;
  onKey: (event: KeyboardEvent) => void;
}

/**
 * Generates a
 * [tab list element](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/)
 * and its child tab elements with all required ARIA attributes.
 * All tab elements have an `aria-controls` attribute that references the
 * associated tab panel.
 *
 * The tab panels controlled by the tab list are made known by objects of
 * type {@link AashTablist.Panel Panel}. Because the tab panels are
 * referenced from the tab elements, the tab panel elements need only
 * an `id` attribute and `role=tabpanel` `tabindex=0`.
 *
 * The component exposes public methods for panel management:
 * `addPanel(panel)`, `removePanel(id)`, `selectPanel(id)`, and `panels()`.
 *
 * The DOM is generated as shown in the
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/tabs/tabs-2/tabs.html)
 *
 * Example:
 * ```html
 * <aash-tablist id="sampleTabs"></aash-tablist>
 * <div id="tab-1" role="tabpanel">This is panel One.</div>
 * <div id="tab-2" role="tabpanel" hidden="">This is panel Two.</div>
 * <script>
 *   const tablist = document.getElementById('sampleTabs');
 *   tablist.addPanel({ id: 'tab-1', label: 'Tab 1' });
 *   tablist.addPanel({ id: 'tab-2', label: 'Tab 2' });
 * </script>
 * ```
 *
 * @class AashTablist
 */
export class AashTablist extends HTMLElement {

    /** @internal Reactive state managed by AlpineJS */
    private readonly ajsData: TablistData;

    constructor() {
        super();
        this.ajsData = Alpine.reactive(this.tablistAlpineData());
    }

    /**
     * @internal AlpineJS data definition for the tablist component.
     */
    protected tablistAlpineData(): TablistData {
        const element = this;

        return {
            panels: [] as Panel[],
            selected: null,
            l10n: null as ((key: string) => string) | null,

            get isVertical() {
                return element.getAttribute("aria-orientation")
                    === "vertical";
            },

            selectPanel(panelId: string) {
                if (this.selected) {
                    let tabpanel = document.querySelector(
                        "[id='" + this.selected + "']");
                    if (tabpanel) {
                        tabpanel.setAttribute("hidden", "");
                    }
                }
                this.selected = panelId;
                let tabpanel = document.querySelector(
                    "[id='" + this.selected + "']");
                if (tabpanel) {
                    tabpanel.removeAttribute("hidden");
                }
            },

            label(panel: Panel): string {
                if (typeof panel.label === 'function') {
                    return panel.label();
                }
                if (this.l10n) {
                    return this.l10n(panel.label);
                }
                return panel.label;
            },

            onKey(event: KeyboardEvent) {
                if (event.type === "keydown") {
                    if (this.isVertical
                        && ["ArrowUp", "ArrowDown"]
                            .includes(event.key)) {
                        event.preventDefault();
                    }
                    return;
                }
                if (event.type !== "keyup") {
                    return;
                }

                let panelIndex = -1;
                for (let i = 0; i < this.panels.length; i++) {
                    if (this.panels[i].id === this.selected) {
                        panelIndex = i;
                        break;
                    }
                }
                if (panelIndex < 0) {
                    return;
                }

                let handled = false;
                const panel = this.panels[panelIndex];

                if ((this.isVertical && event.key === "ArrowUp")
                    || (!this.isVertical && event.key === "ArrowLeft")) {
                    this.selectPanel(this.panels[
                        (panelIndex - 1 + this.panels.length)
                            % this.panels.length].id);
                    handled = true;
                } else if ((this.isVertical && event.key === "ArrowDown")
                    || (!this.isVertical
                        && event.key === "ArrowRight")) {
                    this.selectPanel(this.panels[
                        (panelIndex + 1) % this.panels.length].id);
                    handled = true;
                } else if (event.key === "Delete") {
                    if (panel.removeCallback) {
                        panel.removeCallback();
                        handled = true;
                    }
                } else if (event.key === "Home") {
                    this.selectPanel(this.panels[0].id);
                    handled = true;
                } else if (event.key === "End") {
                    this.selectPanel(
                        this.panels[this.panels.length - 1].id);
                    handled = true;
                }

                if (handled) {
                    event.preventDefault();
                    const tab = document.querySelector(
                        "[id='" + this.selected + "-tab'] > button");
                    (tab as HTMLElement)?.focus();
                }
            }
        };
    }

    /**
     * @internal Sets up external tabpanel elements with required attributes.
     */
    protected setupTabpanels() {
        const data = this.ajsData;
        if (data.panels.length > 0 && data.selected === null) {
            data.selectPanel(data.panels[0].id);
        }
        for (const panel of data.panels) {
            this.setupTabpanel(panel.id);
        }
    }

    protected setupTabpanel(panelId: string): HTMLElement | null {
        const tabpanel: HTMLElement | null
            = document.querySelector("[id='" + panelId + "']");
        if (!tabpanel) {
            return null;
        }
        tabpanel.setAttribute("role", "tabpanel");
        tabpanel.setAttribute("aria-labelledby",
            tabpanel.getAttribute('id') + '-tab');
        if (tabpanel.getAttribute('id') === this.ajsData.selected) {
            tabpanel.removeAttribute("hidden");
        } else {
            tabpanel.setAttribute("hidden", "");
        }
        return tabpanel;
    }
    
    protected connectedCallback() {
        // Required because defining
        // "interface TablistData extends Record<string, unknown>"
        // instead would accept access with arbitrary keys 
        Alpine.addScopeToNode(this,
            this.ajsData as unknown as Record<string, unknown>);
        this.render();
        Alpine.initTree(this);
        this.setupTabpanels();
    }

    /** @internal Renders tab strip as HTML with Alpine directives. */
    protected render() {
        this.innerHTML =
            `<div class="aash-tablist" role="tablist"
                x-bind:aria-orientation="isVertical ? 'vertical' : 'horizontal'"
                @keydown="onKey" @keyup="onKey" x-cloak>
                <template x-for="panel of panels">
                    <span x-bind:id="panel.id + '-tab'" role="tab"
                        x-bind:aria-selected="panel.id
                            == selected ? 'true' : 'false'"
                        x-bind:aria-controls="panel.id">
                        <button type="button"
                            x-bind:tabindex="panel.id == selected ? 0 : -1"
                            @click="selectPanel(panel.id)"
                            x-text="label(panel)">
                        </button><button type="button" tabindex="-1"
                            class="aash-tablist-remove"
                            x-show="!!panel.removeCallback"
                            @click="panel.removeCallback()">
                            &times;
                        </button>
                    </span>
                </template>
            </div>`;
    }

    /**
     * Sets the localization function to apply to panel labels
     * before rendering.
     */
    set l10n(value: ((key: string) => string) | null) {
        this.ajsData.l10n = value;
    }

    /**
     * Adds a panel to the tablist.
     * @param panel the panel to add
     */
    addPanel(panel: Panel): void {
        const tabpanel = this.setupTabpanel(panel.id);
        if (!tabpanel) {
            return;
        }
        this.ajsData.panels.push(panel);
        if (this.ajsData.selected === null) {
            this.ajsData.selectPanel(panel.id);
        }
    }

    /**
     * Removes the panel with the given id.
     * @param panelId the id of the panel to remove
     */
    removePanel(panelId: string): void {
        const tabpanel: HTMLElement | null
            = document.querySelector("[id='" + panelId + "']");
        if (tabpanel) {
            tabpanel.setAttribute("hidden", "");
        }
        let prevPanel = 0;
        for (let i = 0; i < this.ajsData.panels.length; i++) {
            if (this.ajsData.panels[i].id === panelId) {
                this.ajsData.panels.splice(i, 1);
                break;
            }
            prevPanel = i;
        }
        if (this.ajsData.panels.length > 0) {
            this.ajsData.selectPanel(this.ajsData.panels[prevPanel].id);
        }
    }

    /**
     * Selects (activates) the panel with the given id.
     * @param panelId the id of the panel to select
     */
    selectPanel(panelId: string): void {
        this.ajsData.selectPanel(panelId);
    }

    /**
     * Returns a copy of the current panels array.
     */
    panels(): Panel[] {
        return this.ajsData.panels.slice();
    }
}

customElements.define('aash-tablist', AashTablist);
