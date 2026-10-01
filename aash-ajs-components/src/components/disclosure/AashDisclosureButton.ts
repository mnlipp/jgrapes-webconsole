/**
 * Provides an element that controls the visibility of another element.
 *
 * @module AashDisclosureButton
 */
import Alpine from 'alpinejs';
import {
  AashComponent,
  Attribute,
  Renderer,
  expressionConverter
} from '../../AashUtil';

/** @internal Maps idRef → reactive disclosure state */
const disclosureStates = new Map<string, { disclosed: boolean }>();

/**
 * @internal AlpineJS data definition for the disclosure component.
 */
interface DisclosureData {
  idRef: string;
  disclosed: boolean;
  onShow: (() => void) | null;
  onHide: (() => void) | null;
  onToggle: ((value: boolean) => void) | null;
  toggleDisclosed: () => void;
}

/**
 * Generates an element (a `button` by default) that toggles the
 * visibility of another element when clicked.
 *
 * Example:
 * ```html
 * <aash-disclosure-button id-ref="onlyShownWhenDisclosed">
 *   Disclose
 * </aash-disclosure-button>
 * <div id="onlyShownWhenDisclosed">Secret content</div>
 * ```
 *
 * @class AashDisclosureButton
 */
@AashComponent()
export class AashDisclosureButton extends HTMLElement {

    /** @internal Reactive state managed by AlpineJS */
    private readonly ajsData: DisclosureData;

    constructor() {
        super();
        this.ajsData = Alpine.reactive(this.disclosureButtonAlpineData());
    }

    /**
     * @internal AlpineJS data definition for the disclosure component.
     */
    protected disclosureButtonAlpineData(): DisclosureData {
        const element = this;

        return {
            idRef: '',
            disclosed: false,
            onShow: null,
            onHide: null,
            onToggle: null,

            toggleDisclosed() {
                this.disclosed = !this.disclosed;
                if (this.onToggle) {
                    this.onToggle(this.disclosed);
                }
                if (this.disclosed) {
                    if (this.onShow) {
                        this.onShow();
                    }
                } else {
                    if (this.onHide) {
                        this.onHide();
                    }
                }
            }
        };
    }

    protected connectedCallback() {
        const idRef = this.getAttribute('id-ref') || '';
        if (idRef) {
            /* Share state across buttons controlling the same element. */
            if (disclosureStates.has(idRef)) {
                const existing = disclosureStates.get(idRef);
                /* Copy disclosed value from existing state. */
                if (existing) {
                    this.ajsData.disclosed = existing.disclosed;
                }
            } else {
                disclosureStates.set(idRef, {
                    disclosed: this.ajsData.disclosed
                });
            }
        }
        Alpine.addScopeToNode(this,
            this.ajsData as unknown as Record<string, unknown>);
        this.render();
        Alpine.initTree(this);
    }

    /** @internal Renders as a button with Alpine directives. */
    @Renderer()
    protected render() {
        this.innerHTML =
            `<button type="button"
                data-aash-role="disclosure-button"
                x-bind:aria-expanded="disclosed ? 'true' : 'false'"
                x-bind:aria-controls="idRef"
                @click="toggleDisclosed()">
                <slot></slot>
            </button>`;
    }

    /**
     * The id of the controlled element.
     */
    @Attribute('id-ref')
    set idRef(value: string) {
        this.ajsData.idRef = value;
    }

    /**
     * Callback invoked when the controlled element is shown.
     */
    @Attribute('on-show', expressionConverter)
    set onShow(value: (() => void) | null) {
        this.ajsData.onShow = value;
    }

    /**
     * Callback invoked when the controlled element is hidden.
     */
    @Attribute('on-hide', expressionConverter)
    set onHide(value: (() => void) | null) {
        this.ajsData.onHide = value;
    }

    /**
     * Callback invoked on every toggle with the new disclosed state.
     */
    @Attribute('on-toggle', expressionConverter)
    set onToggle(value: ((value: boolean) => void) | null) {
        this.ajsData.onToggle = value;
    }

    disconnectedCallback() {
        const idRef = this.ajsData.idRef;
        /* Clean up shared state only if no other button references it. */
        if (idRef) {
            const refs = document.querySelectorAll(
                `aash-disclosure-button[id-ref="${idRef}"]`);
            if (refs.length <= 1) {
                disclosureStates.delete(idRef);
            }
        }
    }
}

customElements.define('aash2-disclosure-button', AashDisclosureButton);
