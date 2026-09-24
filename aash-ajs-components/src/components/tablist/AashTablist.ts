/**
 * Provides tablist element.
 * @module AashTablist
 */
import Alpine from 'alpinejs';
import {
  AashComponent, Attribute, Renderer, aashId
} from '../../AashUtil';

/**
 * @internal AlpineJS data definition for the tablist component.
 */
interface TablistData {
  panelIds: string[];
  selected: string | null;
  isVertical: boolean;
  removeCallbacks: Map<string, (() => void) | null>;
  selectPanel: (panelId: string) => void;
  onKey: (event: KeyboardEvent) => void;
}

/**
 * Generates a
 * [tab list element](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/)
 * and its child tab elements with all required ARIA attributes.
 * All tab elements have an `aria-controls` attribute that references the
 * associated tab panel.
 *
 * Tabs are defined via `<template provides="tab">` children with a
 * `panel-id` attribute referencing the controlled panel.
 * The template content becomes the tab label.
 * An optional `remove-callback` attribute is evaluated as an Alpine
 * expression.
 *
 * The DOM is generated as shown in the
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/tabs/tabs-2/tabs.html)
 *
 * Example:
 * ```html
 * <aash-tablist id="sampleTabs">
 *   <template provides="tab" panel-id="tab-1">
 *     <span>Tab 1</span>
 *   </template>
 *   <template provides="tab" panel-id="tab-2">
 *     <span>Tab 2</span>
 *   </template>
 * </aash-tablist>
 * <div id="tab-1" role="tabpanel">This is panel One.</div>
 * <div id="tab-2" role="tabpanel" hidden>This is panel Two.</div>
 * ```
 *
 * @class AashTablist
 */
@AashComponent()
export class AashTablist extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: TablistData;
  /** @internal Observer for template children changes */
  private contentObserver = new MutationObserver(() => {
      Alpine.nextTick(() => this.updateContent());
  });

  constructor() {
    super();
    const element = this;
    this.ajsData = Alpine.reactive({
      panelIds: [] as string[],
      selected: null as string | null,
      removeCallbacks: new Map<string, (() => void) | null>(),

      get isVertical() {
        return element.getAttribute("aria-orientation") === "vertical";
      },

      selectPanel(panelId: string) {
        if (this.selected) {
          const tabpanel = document.querySelector("[id='" + this.selected + "']");
          if (tabpanel) {
            tabpanel.setAttribute("hidden", "");
          }
        }
        this.selected = panelId;
        const tabpanel = document.querySelector("[id='" + this.selected + "']");
        if (tabpanel) {
          tabpanel.removeAttribute("hidden");
        }
      },

      onKey(event: KeyboardEvent) {
        if (event.type === "keydown") {
          if (this.isVertical && ["ArrowUp", "ArrowDown"].includes(event.key)) {
            event.preventDefault();
          }
          return;
        }
        if (event.type !== "keyup") {
          return;
        }

        const panelIds = this.panelIds;
        let panelIndex = -1;
        for (let i = 0; i < panelIds.length; i++) {
          if (panelIds[i] === this.selected) {
            panelIndex = i;
            break;
          }
        }
        if (panelIndex < 0) {
          return;
        }

        let handled = false;
        const callback = this.selected
          ? element.ajsData.removeCallbacks.get(this.selected)
          : null;

        if ((this.isVertical && event.key === "ArrowUp")
            || (!this.isVertical && event.key === "ArrowLeft")) {
          this.selectPanel(panelIds[(panelIndex - 1 + panelIds.length) % panelIds.length]);
          handled = true;
        } else if ((this.isVertical && event.key === "ArrowDown")
            || (!this.isVertical && event.key === "ArrowRight")) {
          this.selectPanel(panelIds[(panelIndex + 1) % panelIds.length]);
          handled = true;
        } else if (event.key === "Delete") {
          if (callback) {
            callback();
            handled = true;
          }
        } else if (event.key === "Home") {
          this.selectPanel(panelIds[0]);
          handled = true;
        } else if (event.key === "End") {
          this.selectPanel(panelIds[panelIds.length - 1]);
          handled = true;
        }

        if (handled) {
          event.preventDefault();
          const tab = document.querySelector(
            "[id='" + this.selected + "-tab'] > button");
          (tab as HTMLElement)?.focus();
        }
      }
    } satisfies TablistData);
  }

  /** @internal Renders tab strip as HTML with Alpine directives. */
  @Renderer()
  protected render() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    const shown = this.ownerDocument.createRange().createContextualFragment(
      `<div class="aash-tablist" role="tablist"
          x-bind:aria-orientation="isVertical ? 'vertical' : 'horizontal'"
          @keydown="onKey" @keyup="onKey" x-cloak>
      </div>`);
    this.prepend(shown);
    this.updateContent();
  }

  /** @internal Teleports template content into structural targets. */
  private updateContent() {
    this.contentObserver.disconnect();
    const tablist = this.querySelector('div[role="tablist"]')!;
    // Remove previously inserted tabs
    tablist.querySelectorAll(':scope > [data-aash-tab]').forEach(el => el.remove());
    // Clear reactive panel data
    this.ajsData.panelIds = [];
    this.ajsData.removeCallbacks = new Map();

    const tabTemplates = this.querySelectorAll(':scope > [provides="tab"]');
    tabTemplates.forEach((tabTemplate, index) => {
      const panelId = tabTemplate.getAttribute('panel-id')!;
      this.ajsData.panelIds.push(panelId);

      const removeCallbackAttr = tabTemplate.getAttribute('remove-callback');
      if (removeCallbackAttr) {
        const callbackExpr = removeCallbackAttr;
        const element = this;
        this.ajsData.removeCallbacks.set(panelId, () => {
          Alpine.evaluate(element, callbackExpr);
        });
      } else {
        this.ajsData.removeCallbacks.set(panelId, null);
      }

      const labelTarget = aashId();
      const removeTarget = aashId();
      const tabId = panelId + '-tab';

      const tabFragment = this.ownerDocument.createRange().createContextualFragment(
        `<span id="${tabId}" role="tab" data-aash-tab
            x-bind:aria-selected="selected == '${panelId}' ? 'true' : 'false'"
            x-bind:aria-controls="'${panelId}'">
          <button type="button" id="${labelTarget}"
              x-bind:tabindex="selected == '${panelId}' ? 0 : -1"
              @click="selectPanel('${panelId}')">
          </button>
          <button type="button" tabindex="-1"
              class="aash-tablist-remove"
              id="${removeTarget}">
          </button>
        </span>`);
      tablist.appendChild(tabFragment);

      tabTemplate.setAttribute("x-teleport", "#" + labelTarget);

      const hasRemoveCallback = !!removeCallbackAttr;
      const removeButton = this.querySelector(`#${removeTarget}`)!;
      if (!hasRemoveCallback) {
        removeButton.setAttribute('hidden', '');
      } else {
        removeButton.setAttribute('@click',
          `removeCallbacks.get('${panelId}')?.()`);
      }

      // Setup tabpanel
      this.setupTabpanel(panelId);

      // Select first panel
      if (index === 0 && this.ajsData.selected === null) {
        this.ajsData.selectPanel(panelId);
      }
    });

    this.contentObserver.observe(this, {
      childList: true,
      subtree: false,
      attributes: false,
      characterData: false,
    });
  }

  /** @internal Sets up a tabpanel element with required attributes. */
  private setupTabpanel(panelId: string): void {
    const tabpanel = document.querySelector(`[id='${panelId}']`);
    if (!tabpanel) {
      return;
    }
    tabpanel.setAttribute("role", "tabpanel");
    tabpanel.setAttribute("aria-labelledby", panelId + '-tab');
    if (tabpanel.getAttribute('id') === this.ajsData.selected) {
      tabpanel.removeAttribute("hidden");
    } else {
      tabpanel.setAttribute("hidden", "");
    }
  }

  disconnectedCallback() {
    this.contentObserver.disconnect();
  }

  /**
   * Selects (activates) the panel with the given id.
   * @param panelId the id of the panel to select
   */
  selectPanel(panelId: string): void {
    this.ajsData.selectPanel(panelId);
  }

  /**
   * Removes the panel with the given id.
   * @param panelId the id of the panel to remove
   */
  removePanel(panelId: string): void {
    const tabpanel = document.querySelector(`[id='${panelId}']`);
    if (tabpanel) {
      tabpanel.setAttribute("hidden", "");
    }
    const panelIds = this.ajsData.panelIds;
    let prevPanel = 0;
    for (let i = 0; i < panelIds.length; i++) {
      if (panelIds[i] === panelId) {
        panelIds.splice(i, 1);
        break;
      }
      prevPanel = i;
    }
    this.ajsData.removeCallbacks.delete(panelId);
    const tabElement = this.querySelector(`[id="${panelId}-tab"]`);
    if (tabElement) {
      tabElement.remove();
    }
    const template = this.querySelector(`[panel-id="${panelId}"]`);
    if (template) {
      template.remove();
    }
    if (panelIds.length > 0) {
      this.ajsData.selectPanel(panelIds[prevPanel]);
    }
  }
}

customElements.define('aash-tablist', AashTablist);
