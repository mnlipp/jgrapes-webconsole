/**
 * Provides tablist element.
 * @module AashTablist
 */
import { LitElement, html } from 'lit';
import { property, state } from 'lit/decorators.js';

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
 * Generates a
 * [tab list element](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/)
 * and its child tab elements with all required ARIA attributes.
 * All tab elements have an `aria-controls` attribute that references the
 * associated tab panel.
 *
 * The tab panels controlled by the tab list are made known by objects of
 * type {@link AashTablist.Panel Panel}. Because the tab panels are
 * referenced from the
 * tab elements, the tab panel elements need only
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
export class AashTablist extends LitElement {

  /** The list of initial panels */
  @property({ type: Array, attribute: 'initial-panels' })
  public initialPanels: Panel[] = [];

  /**
   * A function invoked with a label (of type string) as argument before
   * the label is rendered
   */
  @property({ attribute: 'l10n', converter: { fromAttribute: () => null } })
  public l10n: ((key: string) => string) | null = null;

  @state()
  private selected: string | null = null;

  @state()
  private panelList: Panel[] = [];
  
  protected createRenderRoot() {
    return this;
  }

  private get isVertical(): boolean {
    return this.getAttribute('aria-orientation') === 'vertical';
  }
    
  /** 
   * Adds the given panel. The panel with the given id must already exist.
   */
  public addPanel(panel: Panel): void {
    this.panelList = [...this.panelList, panel];
    this.setupTabpanel(panel);
  }

  public removePanel(panelId: string): void {
    let prevPanel = 0;
    for (let i = 0; i < this.panelList.length; i++) {
      if (this.panelList[i].id === panelId) {
        this.panelList = this.panelList.filter(p => p.id !== panelId);
        break;
      }
      prevPanel = i;
    }
    if (this.panelList.length > 0) {
      this.selectPanel(this.panelList[prevPanel].id);
    }
  }

  public selectPanel(panelId: string): void {
    if (this.selected) {
      let tabpanel = document.querySelector(`[id='${this.selected}']`);
      if (tabpanel) {
        tabpanel.setAttribute('hidden', '');
      }
    }
    this.selected = panelId;
    let tabpanel = document.querySelector(`[id='${this.selected}']`);
    if (tabpanel) {
      tabpanel.removeAttribute('hidden');
    }
  }

  public panels(): Panel[] {
    return this.panelList.slice();
  }

  private label(panel: Panel): string {
    if (typeof panel.label === 'function') {
      return panel.label();
    }
    if (this.l10n) {
      return this.l10n(panel.label);
    }
    return panel.label;
  }

  private setupTabpanel(panel: Panel): void {
    let tabpanel: HTMLElement | null = document.querySelector(
      `[id='${panel.id}']`);
    if (tabpanel == null) {
      return;
    }
    tabpanel.setAttribute('role', 'tabpanel');
    tabpanel.setAttribute('aria-labelledby',
      tabpanel.getAttribute('id') + '-tab');
    if (tabpanel.getAttribute('id') === this.selected) {
      tabpanel.removeAttribute('hidden');
    } else {
      tabpanel.setAttribute('hidden', '');
    }
  }

  private selectedPanel(): [Panel | null, number] {
    for (let i = 0; i < this.panelList.length; i++) {
      let panel = this.panelList[i];
      if (panel.id === this.selected) {
        return [panel, i];
      }
    }
    return [null, -1];
  }

  private onKeydown(event: KeyboardEvent): void {
    if (this.isVertical
        && ['ArrowUp', 'ArrowDown'].includes(event.key)) {
      event.preventDefault();
    }
  }

  private onKeyup(event: KeyboardEvent): void {
    let [panel, panelIndex] = this.selectedPanel();
    if (!panel) {
      return;
    }
    let handled = false;
    if ((this.isVertical && event.key === 'ArrowUp')
        || (!this.isVertical && event.key === 'ArrowLeft')) {
      this.selectPanel(this.panelList[
        (panelIndex - 1 + this.panelList.length) % this.panelList.length].id);
      handled = true;
    } else if ((this.isVertical && event.key === 'ArrowDown')
        || (!this.isVertical && event.key === 'ArrowRight')) {
      this.selectPanel(this.panelList[
        (panelIndex + 1) % this.panelList.length].id);
      handled = true;
    } else if (event.key === 'Delete') {
      if (panel.removeCallback) {
        panel.removeCallback();
        handled = true;
      }
    } else if (event.key === 'Home') {
      this.selectPanel(this.panelList[0].id);
      handled = true;
    } else if (event.key === 'End') {
      this.selectPanel(this.panelList[this.panelList.length - 1].id);
      handled = true;
    }
    if (handled) {
      event.preventDefault();
      let tab: HTMLElement | null = document.querySelector(
        `[id='${this.selected}-tab'] > button`);
      tab?.focus();
    }
  }

  public connectedCallback(): void {
    super.connectedCallback();
    this.panelList = this.initialPanels.slice();
    if (this.panelList.length > 0) {
      this.selected = this.panelList[0].id;
    }
    for (let panel of this.panelList) {
      this.setupTabpanel(panel);
    }
  }

  public updated(changedProperties: Map<string, any>): void {
    super.updated(changedProperties);
    if (changedProperties.has('panelList')) {
      if (this.selected === null && this.panelList.length > 0) {
        this.selectPanel(this.panelList[0].id);
      }
    }
  }

  protected render() {
    return html`
      <div class="aash-tablist" role="tablist"
        @keydown=${this.onKeydown} @keyup=${this.onKeyup}>
        ${this.panelList.map(panel => html`
          <span id="${panel.id}-tab" role="tab"
            .aria-selected=${panel.id === this.selected ? 'true' : 'false'}
            aria-controls="${panel.id}">
            <button type="button"
              tabindex="${panel.id === this.selected ? 0 : -1}"
              @click=${() => this.selectPanel(panel.id)}>
              ${this.label(panel)}
            </button>
            ${panel.removeCallback
              ? html`<button type="button" class="fa fa-times"
                @click=${panel.removeCallback} tabindex="-1"></button>`
              : ''}
          </span>
        `)}
      </div>
    `;
  }
}

customElements.define('aash-tablist', AashTablist);
