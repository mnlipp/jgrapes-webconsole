/**
 * Provides a section of an accordion.
 *
 * @module AashAccordionSection
 */
import Alpine from 'alpinejs';
import { AashComponent, Attribute, Renderer } from '../../AashUtil';

/** @internal Imports Controller type from parent module */
import type { Controller } from './AashAccordion';

/**
 * @internal AlpineJS data definition for the accordion section.
 */
interface AccordionSectionData {
  sectionId: string;
  index: number;
  ctrl: Controller;
  title: string;
  headerType: string;
  buttonType: string;
  panelType: string;
  panelClass: string | null;
  isExpanded: () => boolean;
  onClick: (event: Event) => void;
  onKey: (event: KeyboardEvent) => void;
}

/**
 * Generates the header and region of an accordion section.
 *
 * The component uses AlpineJS's `x-collapse` directive for the
 * sliding animation of the panel height.
 *
 * Adding padding to the section breaks the transition. An extra `div`
 * is generated as child of the section that contains the slot content.
 *
 * @class AashAccordionSection
 */
@AashComponent()
export class AashAccordionSection extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: AccordionSectionData;

  constructor() {
    super();
    this.ajsData = Alpine.reactive(
      this.accordionSectionAlpineData()
    );
  }

  /**
   * @internal AlpineJS data definition for the accordion section.
   */
  protected accordionSectionAlpineData(): AccordionSectionData {
    const sectionId = generateSectionId();

    return {
      sectionId,
      index: 0,
      ctrl: null as unknown as Controller,
      title: '',
      headerType: 'div',
      buttonType: 'button',
      panelType: 'div',
      panelClass: null,

      isExpanded(): boolean {
        return this.ctrl.isExpanded(this.index);
      },

      onClick(event: Event) {
        this.ctrl.toggleExpanded(this.index);
      },

      onKey(event: KeyboardEvent) {
        if (event.key === 'ArrowDown') {
          this.ctrl.selectNext(this.index);
        } else if (event.key === 'ArrowUp') {
          this.ctrl.selectPrev(this.index);
        } else if (event.key === 'Home') {
          this.ctrl.selectFirst();
        } else if (event.key === 'End') {
          this.ctrl.selectLast();
        }
      }
    } as AccordionSectionData;
  }

  protected connectedCallback() {
    const accordion = this.closest('aash-accordion');
    if (accordion) {
      this.ajsData.ctrl = (accordion as any)._aashCtrl;
    } else {
      throw new Error(
        'aash-accordion-section must be inside aash-accordion'
      );
    }
    this.ajsData.index =
      this.ajsData.ctrl.addSection(this.ajsData.sectionId);

    const accordionEl = accordion!;
    this.ajsData.headerType =
      accordionEl.getAttribute('header-type') || 'div';
    this.ajsData.buttonType =
      accordionEl.getAttribute('button-type') || 'button';
    this.ajsData.panelType =
      accordionEl.getAttribute('panel-type') || 'div';
    this.ajsData.panelClass =
      accordionEl.getAttribute('panel-class') || null;

    Alpine.addScopeToNode(
      this,
      this.ajsData as unknown as Record<string, unknown>
    );
    this.render();
    Alpine.initTree(this);
  }

  /** @internal Renders the section with Alpine directives. */
  @Renderer()
  protected render() {
    const data = this.ajsData;
    const panelId = data.sectionId + '-panel';
    const controlId = data.sectionId + '-control';
    const headerRole =
      !data.headerType.match(/^[hH][1-9][0-9]*$/) ? 'header' : '';

    this.innerHTML =
      `<div>
        <${data.headerType}
          ${headerRole ? `role="header"` : ''}>
          <${data.buttonType}
            x-bind:id="${controlId}"
            tabindex="0"
            ${data.buttonType !== 'button'
              ? 'role="button"' : ''}
            x-bind:aria-controls="${panelId}"
            x-bind:aria-expanded="
              isExpanded() ? 'true' : 'false'"
            @click="onClick" @keydown="onKey">
            <slot name="title">
              <span x-text="title"></span>
            </slot>
          </${data.buttonType}>
        </${data.headerType}>
        <${data.panelType}
          x-bind:id="${panelId}"
          role="region"
          x-bind:class="panelClass"
          x-bind:aria-labelledby="${controlId}"
          x-show="isExpanded()"
          x-collapse
          x-cloak>
          <div><slot></slot></div>
        </${data.panelType}>
      </div>`;
  }

  disconnectedCallback() {
    this.ajsData.ctrl.removeSection(this.ajsData.sectionId);
  }

  /**
   * The title text for the accordion section header.
   */
  @Attribute('title')
  set title(value: string) {
    this.ajsData.title = value;
  }
}

/** @internal Unique ID counter for sections */
let _sectionIdCounter = 0;

/** @internal Generates a unique section ID */
function generateSectionId(): string {
  return `aash-id-${_sectionIdCounter++}`;
}

customElements.define(
  'aash-accordion-section',
  AashAccordionSection
);
