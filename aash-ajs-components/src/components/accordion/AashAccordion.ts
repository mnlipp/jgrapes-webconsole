/**
 * Provides an element that controls the visibility of
 * vertically stacked sections.
 *
 * @module AashAccordion
 */
import Alpine from 'alpinejs';
import {
  AashComponent,
  Attribute,
  aashId,
  booleanConverter,
  Data,
  Renderer
} from '../../AashUtil';

/**
 * @internal Controller class managing accordion state and navigation.
 */
export class Controller {
  private _alwaysExpanded: boolean;
  private _singleExpansion: boolean;
  private _domRoot: HTMLElement | null = null;
  private _sections: string[] = [];
  readonly expanded: Set<number>;

  constructor(alwaysExpanded: boolean, singleExpansion: boolean) {
    this._alwaysExpanded = alwaysExpanded;
    this._singleExpansion = singleExpansion;
    this.expanded = Alpine.reactive(new Set<number>());
  }

  setDomRoot(root: HTMLElement) {
    this._domRoot = root;
  }

  addSection(id: string): number {
    this._sections.push(id);
    if (this._alwaysExpanded && this.expanded.size === 0) {
      this.expanded.add(0);
    }
    return this._sections.length - 1;
  }

  removeSection(id: string) {
    const idx = this._sections.indexOf(id);
    if (idx < 0) {
      return;
    }
    this._sections.splice(idx, 1);
    this.expanded.delete(idx);
    /* Re-map indices after removed section. */
    const oldValues = [...this.expanded];
    this.expanded.clear();
    for (const i of oldValues) {
      if (i < idx) {
        this.expanded.add(i);
      } else if (i > idx && this._sections.length > i - 1) {
        this.expanded.add(i - 1);
      }
    }
    if (this._alwaysExpanded && this.expanded.size === 0) {
      this.expanded.add(0);
    }
  }

  isExpanded(index: number): boolean {
    return this.expanded.has(index);
  }

  expand(index: number) {
    if (this.expanded.has(index)) {
      return;
    }
    this.toggleExpanded(index);
  }

  toggleExpanded(index: number) {
    if (this.expanded.has(index)) {
      if (this._alwaysExpanded && this.expanded.size === 1) {
        return;
      }
      this.expanded.delete(index);
      return;
    }
    if (this._singleExpansion) {
      this.expanded.clear();
    }
    this.expanded.add(index);
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
      const sectionId = this._sections[index];
      const target = this._domRoot?.querySelector(
        `summary[data-section-id="${sectionId}"]`
      );
      (target as HTMLElement | null)?.focus();
    });
  }
}

/**
 * @classdesc
 * Generates an accordion.
 *
 * The primary usage is native {@code <details>}/{@code <summary>} elements
 * as direct children. The accordion progressively enhances them with
 * ARIA attributes and shared expansion state.
 *
 * Example source:
 * ```html
 * <aash-accordion always-expanded>
 *   <details>
 *     <summary>Header 1</summary>
 *     Panel 1
 *   </details>
 *   <details>
 *     <summary>Header 2</summary>
 *     Panel 2
 *   </details>
 * </aash-accordion>
 * ```
 *
 * For backward compatibility, {@code aash-accordion-section} elements are
 * also supported. Each such element generates a {@code <details>} structure
 * with a nested {@code <summary>}.
 *
 * @class AashAccordion
 */
@AashComponent()
export class AashAccordion extends HTMLElement {

  /** @internal Controller instance for child sections */
  private readonly ctrl: Controller;
  /** @internal Observer for details children changes */
  private contentObserver = new MutationObserver(() => {
    Alpine.nextTick(() => this.updateContent());
  });

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
  }

  @Data()
  protected data(): object {
    const _this = this;
    return {
      isExpanded: (index: number) => _this.ctrl.isExpanded(index)
    }
  }
  
  /** @internal Adds scope and triggers initial content update. */
  @Renderer()
  protected render() {
    this.updateContent();
  }

  /**
   * @internal Enhances unattributed details children.
   */
  private updateContent() {
    this.contentObserver.disconnect();
    const detailsElements = this.querySelectorAll(':scope > details');
    for (const details of detailsElements) {
      if (!details.hasAttribute('data-section-id')) {
        this.enhanceNativeSection(details as HTMLDetailsElement);
      }
    }
    this.contentObserver.observe(this, {
      childList: true,
      subtree: false
    });
  }

  /** @internal Enhances a single details element. */
  private enhanceNativeSection(details: HTMLDetailsElement) {
    const sectionId = aashId();
    const index = this.ctrl.addSection(sectionId);
    let contentId = details.getAttribute('id');
    if (!contentId) {
        contentId = sectionId + '-content';
        details.setAttribute('id', contentId);
    }
    details.setAttribute('data-section-id', sectionId);
    details.setAttribute('x-bind:open', `isExpanded(${index}) ? '' : null`);

    let summary = details.querySelector<HTMLElement>('summary');
    if (!summary) {
      summary = this.ownerDocument.createElement('summary');
      details.prepend(summary);
    }
    details.setAttribute('data-section-index', String(index));
    const controlId = sectionId + '-control';
    summary.setAttribute('id', controlId);
    summary.setAttribute('role', 'button');
    summary.setAttribute('x-bind:aria-expanded',
      `isExpanded(${index}) ? 'true' : 'false'`);
    summary.setAttribute('aria-controls', contentId);
    summary.addEventListener('click', (event: Event) => {
      event.preventDefault();
      this.ctrl.toggleExpanded(index);
    });

    summary.addEventListener('keydown', (event: KeyboardEvent) => {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.ctrl.selectNext(index);
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.ctrl.selectPrev(index);
      } else if (event.key === 'Home') {
        event.preventDefault();
        this.ctrl.selectFirst();
      } else if (event.key === 'End') {
        event.preventDefault();
        this.ctrl.selectLast();
      }
    });
  }

  disconnectedCallback() {
    this.contentObserver.disconnect();
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

customElements.define('aash2-accordion', AashAccordion);
