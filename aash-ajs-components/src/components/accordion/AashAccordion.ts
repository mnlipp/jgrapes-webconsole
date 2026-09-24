/**
 * Provides an element that controls the visibility of
 * vertically stacked sections.
 *
 * @module AashAccordion
 */
import Alpine from 'alpinejs';
import { AashComponent, Attribute, booleanConverter } from '../../AashUtil';

/**
 * The interface provided by the component.
 *
 * `expand(index: number): void` ensures that the section is expanded.
 *
 * @memberof module:AashAccordion
 */
export interface Api {
  expand(index: number): void;
}

/**
 * @internal Controller class managing accordion state and navigation.
 */
export class Controller {
  private _alwaysExpanded: boolean;
  private _singleExpansion: boolean;
  private _domRoot: HTMLElement | null = null;
  private _sections: string[] = [];
  private _expanded: Set<number> = new Set();

  constructor(alwaysExpanded: boolean, singleExpansion: boolean) {
    this._alwaysExpanded = alwaysExpanded;
    this._singleExpansion = singleExpansion;
  }

  setDomRoot(root: HTMLElement) {
    this._domRoot = root;
  }

  addSection(id: string): number {
    this._sections.push(id);
    if (this._alwaysExpanded && this._expanded.size === 0) {
      this._expanded.add(0);
    }
    return this._sections.length - 1;
  }

  removeSection(id: string) {
    const idx = this._sections.indexOf(id);
    if (idx < 0) {
      return;
    }
    this._sections.splice(idx, 1);
    this._expanded.delete(idx);
    /* Re-map indices after removed section. */
    const newExpanded = new Set<number>();
    for (const i of this._expanded) {
      if (i < idx) {
        newExpanded.add(i);
      } else if (i > idx && this._sections.length > i - 1) {
        newExpanded.add(i - 1);
      }
    }
    this._expanded = newExpanded;
    if (this._alwaysExpanded && this._expanded.size === 0) {
      this._expanded.add(0);
    }
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
      if (this._alwaysExpanded && this._expanded.size === 1) {
        return;
      }
      this._expanded.delete(index);
      return;
    }
    if (this._singleExpansion) {
      this._expanded.clear();
    }
    this._expanded.add(index);
  }

  selectNext(index: number) {
    if (index >= this._sections.length - 1) {
      return;
    }
    this._updateDomFocus(index + 1);
  }

  selectPrev(index: number) {
    if (index <= 0) {
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
    requestAnimationFrame(() => {
      const target = this._domRoot?.querySelector(
        `#${this._sections[index]}-control`
      );
      (target as HTMLElement | null)?.focus();
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
 * <aash-accordion always-expanded>
 *   <aash-accordion-section title="Header 1">
 *     Panel 1
 *   </aash-accordion-section>
 *   <aash-accordion-section title="Header 2">
 *     Panel 2
 *   </aash-accordion-section>
 * </aash-accordion>
 * ```
 *
 * The resulting DOM follows the example shown in the
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/accordion/accordion.html).
 * Notable differences are a `div` surrounding the pairs of headers and panels
 * and an additional div between the panel and its content.
 *
 * The former simplifies styling of a section independent of its expanded state
 * with e.g. a shadow.
 *
 * The latter is required for animating accordions. Details can be found in
 * the documentation of `AashAccordionSection`.
 *
 * @class AashAccordion
 */
@AashComponent()
export class AashAccordion extends HTMLElement {

  /** @internal Controller instance for child sections */
  private readonly ctrl: Controller;

  constructor() {
    super();
    this.ctrl = new Controller(
      this.hasAttribute('always-expanded'),
      !this.hasAttribute('multiple-expansion')
    );
  }

  protected connectedCallback() {
    /* Expose controller for child sections. */
    (this as unknown as Record<string, unknown>)._aashCtrl = this.ctrl;
    this.ctrl.setDomRoot(this);
    Alpine.initTree(this);
  }

  /**
   * Ensures that the section at the given index is expanded.
   * @param index the section index
   */
  expand(index: number): void {
    this.ctrl.expand(index);
  }

  /**
   * Whether at least one section must always be expanded.
   * Only effective before the controller is created (in constructor).
   */
  @Attribute('always-expanded', booleanConverter)
  set alwaysExpanded(_value: boolean) {
    /* Runtime change has no effect on existing controller. */
  }

  /**
   * Whether multiple sections may be expanded simultaneously.
   * Only effective before the controller is created (in constructor).
   */
  @Attribute('multiple-expansion', booleanConverter)
  set multipleExpansion(_value: boolean) {
    /* Runtime change has no effect on existing controller. */
  }
}

customElements.define('aash-accordion', AashAccordion);
