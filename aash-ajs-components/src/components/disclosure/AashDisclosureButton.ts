/**
 * Provides an element that controls the visibility of another element.
 *
 * @module AashDisclosureButton
 */
import Alpine from 'alpinejs';
import {
  AashComponent, Attribute, Data, Renderer, expressionConverter,
  aashGlobals, aashId
} from '../../AashUtil';

let disclosureRegistry = Alpine.reactive(new Map<string, boolean>());

interface AashGlobals {
    /**
     * Returns the state of the disclosure with the given `id`.
     * @function
     * @param id the id
     */
    isDisclosed(id: string): boolean;
    
    /**
     * Remove the discloure data for the given `id`.
     * @function
     * @param id the id
     */
    removeDisclosure(id: string): void;
}

(aashGlobals as AashGlobals).isDisclosed = (id: string) => {
    return disclosureRegistry.get(id) || false;
},

(aashGlobals as AashGlobals).removeDisclosure = (id: string) => {
    return disclosureRegistry.delete(id);
}

/** @internal Maps idRef → reactive disclosure state */
const disclosureStates = new Map<string, { disclosed: boolean }>();

/**
 * @internal AlpineJS data definition for the disclosure component.
 */
interface DisclosureData {
  idRef: string;
  disclosed: boolean;
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
    private wrapper: string = 'button';
    private onShowCb: (() => void) | null = null;
    private onHideCb: (() => void) | null = null;
    private onToggleCb: ((value: boolean) => void) | null = null;

    constructor() {
        super();
        const _this = this;
        this.ajsData = Alpine.reactive({
            idRef: '',
            get disclosed() {
                return _this.isDisclosed;
            },
            toggleDisclosed: () => _this.toggleDisclosed()
        } satisfies DisclosureData);
    }

    @Data()
    protected data(): object {
        return this.ajsData;
    }
    
    /** @internal Renders as a button with Alpine directives. */
    @Renderer()
    protected render() {
        if(this.ajsData.idRef === '') {
            this.ajsData.idRef = aashId();
        }
        
        const content = this.innerHTML;
        this.innerHTML =
            `<${this.wrapper} ${this.wrapper === "button"
                ? "type='button'" : "aria-role='button' tabindex='0'"}
                data-aash-role="disclosure-button"
                x-bind:aria-expanded="disclosed ? 'true' : 'false'"
                x-bind:aria-controls="idRef"
                @click="toggleDisclosed()">
            </button>`;
        this.firstElementChild!.innerHTML = content;
    }

    get isDisclosed() {
        return disclosureRegistry.get(this.ajsData.idRef) || false;
    }
    
    toggleDisclosed() {
        const disclosed = !this.isDisclosed;
        disclosureRegistry.set(this.ajsData.idRef, disclosed);
        if (this.onToggleCb) {
            this.onToggleCb(disclosed);
        }
        if (disclosed) {
            if (this.onShowCb) {
                this.onShowCb();
            }
        } else {
            if (this.onHideCb) {
                this.onHideCb();
            }
        }
    }
    
    /**
     * The id of the controlled element.
     */
    @Attribute('id-ref')
    set idRef(value: string) {
        this.ajsData.idRef = value;
    }

    /**
     * The type of the controlling element.
     */
    @Attribute('wrapper-type')
    set wrapperType(value: string) {
        this.wrapper = value;
    }

    /**
     * Callback invoked when the controlled element is shown.
     */
    @Attribute('on-show', expressionConverter)
    set onShow(value: (() => void) | null) {
        this.onShowCb = value;
    }

    /**
     * Callback invoked when the controlled element is hidden.
     */
    @Attribute('on-hide', expressionConverter)
    set onHide(value: (() => void) | null) {
        this.onHideCb = value;
    }

    /**
     * Callback invoked on every toggle with the new disclosed state.
     */
    @Attribute('on-toggle', expressionConverter)
    set onToggle(value: ((value: boolean) => void) | null) {
        this.onToggleCb = value;
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
