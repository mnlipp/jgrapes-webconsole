/**
 * Provides tablist element.
 * @module AashTablist
 */
import Alpine from 'alpinejs';
import {
  AashComponent, Attribute, Renderer, aashId
} from '../../AashUtil';

interface PanelData {
  panelId: string;
  removeCallback: string | null;
  labelTarget: string;
}

/**
 * @internal AlpineJS data definition for the tablist component.
 */
interface TablistData {
  panels: PanelData[];
  selected: string | null;
  isVertical: boolean;
  removeLabel: string;
  selectPanel: (panelId: string) => void;
  onKey: (event: KeyboardEvent) => void;
  teleportLabel(panelId: string, labelTarget: string): void;
  runRemoveCallback(callback: string): void;
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
 * An additional template with `provides="remove-label"` can be used to
 * provide a label for the remove button.
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
      panels: [],
      selected: null as string | null,
      removeLabel: "<span>X</span>",

      get isVertical() {
        return element.isVertical;
      },

      selectPanel(panelId: string) {
        element.selectPanel(panelId);
      },

      onKey(event: KeyboardEvent) {
        element.onKey(event);
      },
      
      teleportLabel(panelId: string, labelTarget: string) {
        const tabTemplate = element
          .querySelector(`:scope > template[panel-id='${panelId}']`);
        // Ignored if added during current evaluation
        Alpine.nextTick(
            () => tabTemplate?.setAttribute("x-teleport", "#" + labelTarget));
      },
      
      runRemoveCallback(callback: string) {
        if (callback) {
            Alpine.evaluate(element, callback);
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
          :aria-orientation="isVertical ? 'vertical' : 'horizontal'"
          @keydown="onKey" @keyup="onKey" x-cloak>
        <template x-for="panel of panels" :key="panel.panelId">
          <span :id="panel.panelId + '-tab'" role="tab" data-aash-tab
            :aria-selected="selected == panel.panelId ? 'true' : 'false'"
            :aria-controls="panel.panelId">
            <button type="button" :id="panel.labelTarget"
              :tabindex="selected == panel.panelId ? 0 : -1"
              @click="selectPanel(panel.panelId)"
              x-init="teleportLabel(panel.panelId, panel.labelTarget)">
            </button>
            <button type="button" tabindex="-1"
              class="aash-tablist-remove" x-show="!!panel.removeCallback"
              @click="runRemoveCallback(panel.removeCallback)"
              x-html="removeLabel">
            </button>
          </span>
        </template>
      </div>`);
    this.prepend(shown);
    this.updateContent();
  }

  /** @internal Update panel "registry". */
  private updateContent() {
    this.contentObserver.disconnect();

    // Update remove label
    const removeLabelTemplate 
      = this.querySelector(':scope > template[provides="remove-label"]');
    if (removeLabelTemplate) {
      this.ajsData.removeLabel = removeLabelTemplate.innerHTML;
    }
        
    // Update reactive panel infos
    this.ajsData.panels = [];
    const tabTemplates 
      = this.querySelectorAll(':scope > template[provides="tab"]');
    tabTemplates.forEach((tabTemplate, index) => {
      const panelId = tabTemplate.getAttribute('panel-id');
      if (panelId === null) {
          return;
      }
      const labelTarget = tabTemplate.getAttribute('x-teleport')
        || aashId();
      const removeCallback = tabTemplate.getAttribute('remove-callback');
      this.ajsData.panels.push({ panelId, removeCallback,
        labelTarget });

      // Setup tabpanel
      this.setupTabpanel(panelId);

      // Maybe select first panel
      if (index === 0 && this.ajsData.selected === null) {
        this.selectPanel(panelId);
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

  get isVertical() {
    return this.getAttribute("aria-orientation") === "vertical";
  }
  
  /**
   * Selects (activates) the panel with the given id.
   * @param panelId the id of the panel to select
   */
  selectPanel(panelId: string): void {
    if (this.ajsData.selected) {
      const tabpanel 
        = document.querySelector("[id='" + this.ajsData.selected + "']");
      if (tabpanel) {
        tabpanel.setAttribute("hidden", "");
      }
    }
    this.ajsData.selected = panelId;
    const tabpanel 
      = document.querySelector("[id='" + this.ajsData.selected + "']");
    if (tabpanel) {
      tabpanel.removeAttribute("hidden");
    }
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
    
    const panels = this.ajsData.panels;
    let prevPanel = 0;
    for (let i = 0; i < panels.length; i++) {
      if (panels[i].panelId === panelId) {
        panels.splice(i, 1);
        break;
      }
      prevPanel = i;
    }
    
    const template 
        = this.querySelector(`:scope > template[panel-id="${panelId}"]`);
    if (template) {
      template.remove();
    }
    
    if (panels.length > 0) {
      this.selectPanel(panels[prevPanel].panelId);
    }
  }
  
  private onKey(event: KeyboardEvent) {
    const isVertical = this.isVertical;
    if (event.type === "keydown") {
      if (isVertical && ["ArrowUp", "ArrowDown"].includes(event.key)) {
          event.preventDefault();
      }
      return;
    }
    if (event.type !== "keyup") {
      return;
    }

    const panels = this.ajsData.panels;
    let panelIndex = -1;
    for (let i = 0; i < panels.length; i++) {
      if (panels[i].panelId === this.ajsData.selected) {
        panelIndex = i;
        break;
      }
    }
    if (panelIndex < 0) {
      return;
    }

    let handled = false;

    if ((isVertical && event.key === "ArrowUp")
      || (!isVertical && event.key === "ArrowLeft")) {
      this.selectPanel(panels[(panelIndex - 1 
          + panels.length) % panels.length].panelId);
      handled = true;
    } else if ((isVertical && event.key === "ArrowDown")
        || (!isVertical && event.key === "ArrowRight")) {
      this.selectPanel(panels[(panelIndex + 1) % panels.length].panelId);
      handled = true;
    } else if (event.key === "Delete") {
      const callback = panels[panelIndex].removeCallback;
      if (callback) {
        Alpine.evaluate(this, callback);
        handled = true;
      }
    } else if (event.key === "Home") {
      this.selectPanel(panels[0].panelId);
      handled = true;
    } else if (event.key === "End") {
      this.selectPanel(panels[panels.length - 1].panelId);
      handled = true;
    }

    if (handled) {
      event.preventDefault();
      const tab = document.querySelector(
          "[id='" + this.ajsData.selected + "-tab'] > button");
      (tab as HTMLElement)?.focus();
    }
  }
}

customElements.define('aash-tablist', AashTablist);
