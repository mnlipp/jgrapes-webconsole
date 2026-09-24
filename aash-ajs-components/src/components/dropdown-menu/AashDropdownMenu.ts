/**
 * Provides dropdown-menu element.
 * @module AashDropdownMenu
 */
import Alpine from 'alpinejs';
import {
  AashComponent,
  Attribute,
  Renderer,
  expressionConverter,
  aashId
} from '../../AashUtil';

/**
 * @internal AlpineJS data definition for the dropdown-menu component.
 */
interface DropdownMenuData {
  expanded: boolean;
  toggle: (event: MouseEvent) => void;
  chooseItem: (value: string) => void;
  close: () => void;
}

/**
 * Generates a dropdown menu with all required ARIA attributes.
 *
 * The DOM is generated as shown in the
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/menu-button/menu-button-actions.html)
 *
 * Example:
 * ```html
 * <aash-dropdown-menu id="language-selector">
 *   <template provides="label">
 *     <span>Language</span>
 *   </template>
 *   <template provides="item" with-value="en">
 *     <span>English</span>
 *   </template>
 *   <template provides="item" with-value="de">
 *     <span>German</span>
 *   </template>
 * </aash-dropdown-menu>
 * ```
 *
 * @class AashDropdownMenu
 */
@AashComponent()
export class AashDropdownMenu extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: DropdownMenuData;
  /** @internal Global click handler for click-outside detection */
  private globalClickHandler:
    | ((event: MouseEvent) => void) | null = null;
  private onSelected: ((value: string) => void) | null = null;
  private lastEvent: MouseEvent | null = null;
  private insertedElements: Set<Element> = new Set();
  private labelTarget = aashId();
  private contentObserver = new MutationObserver(() => {
      Alpine.nextTick(() => this.updateContent());
  });

  constructor() {
    super();
    const element = this;
    this.ajsData = Alpine.reactive({
      expanded: false,

      toggle(event: MouseEvent) {
        this.expanded = !this.expanded;
        if (this.expanded) {
          element.lastEvent = event;
          element.globalClickHandler =
            (evt: MouseEvent) => {
              if (element.lastEvent
                && evt.target !== element.lastEvent.target) {
                this.close();
              }
            };
          document.addEventListener(
            "click", element.globalClickHandler!);
        } else {
          element.removeGlobalClickHandler();
        }
      },

      chooseItem(value: string) {
        if (element.onSelected) {
          element.onSelected(value);
        }
        this.close();
      },

      close() {
        this.expanded = false;
        element.removeGlobalClickHandler();
      }
    } satisfies DropdownMenuData);
  }

  /** @internal Removes the global click handler */
  protected removeGlobalClickHandler() {
    if (this.globalClickHandler) {
      document.removeEventListener(
        "click", this.globalClickHandler);
      this.globalClickHandler = null;
    }
  }

  @Renderer()
  protected render() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    const menuId = this.id + '-menu';
    const shown = this.ownerDocument.createRange().createContextualFragment(
      `<div class="dropdown-menu aash-dropdown-menu" x-cloak>
        <button id="${this.labelTarget}" type="button" aria-haspopup="menu"
            x-bind:aria-controls="'${menuId}'"
            x-bind:aria-expanded="expanded
                ? 'true' : 'false'" @click="toggle">
        </button>
        <ul id="${menuId}" role="menu">
        </ul>
      </div>`);
    this.prepend(shown);
    this.updateContent();
  }

  private updateContent() {
    this.contentObserver.disconnect();
    this.insertedElements.forEach(el => el.remove());
    const button = this.querySelector(':scope button[aria-haspopup="menu"]')!;
    const label = this.querySelector(':scope > [provides="label"]');
    if (label && !label.hasAttribute("x-teleport")) {
      button.textContent = '';
      label.setAttribute("x-teleport", "#" + this.labelTarget);
    }
    const ul = this.querySelector('ul[role="menu"]')!;
    const items = this.querySelectorAll(':scope > [provides="item"]');
    items.forEach(item => {
      const itemTarget = aashId();
      const itemDom = this.ownerDocument.createRange().createContextualFragment(
        `<li role="none">
           <button id="${itemTarget}" type="button" role="menuitem">
           </button>
         </li>`);
      if (item.getAttribute('with-value')) {
        itemDom.querySelector('button')!.setAttribute('@click',
          `chooseItem(${JSON.stringify(item.getAttribute('with-value')!)})`);
      }
      Array.prototype.slice.call(itemDom.children)
          .forEach(el => this.insertedElements.add(el as Element));
      ul.appendChild(itemDom);
      item.setAttribute("x-teleport", "#" + itemTarget);
    });
    this.contentObserver.observe(this, {
      childList: true,
      subtree: false,
      attributes: false,
      characterData: false,
    });
  }
  
  disconnectedCallback() {
    this.removeGlobalClickHandler();
  }

  /**
   * The action callback invoked when a menu item is chosen.
   * Receives the value attribute as argument.
   */
  @Attribute('action', expressionConverter)
  set action(value: ((value: string) => void) | null) {
    this.onSelected = value;
  }
}

customElements.define('aash-dropdown-menu', AashDropdownMenu);

/**
 * Injects the global styles for the dropdown menu once.
 */
const injectStyles = () => {
  if (!document.querySelector('style[aash-dropdown-menu-styles]')) {
    const style = document.createElement('style');
    style.setAttribute('aash-dropdown-menu-styles', '');
    style.textContent = `
.aash-dropdown-menu {
    display: inline-block;
    position: relative;
}

.aash-dropdown-menu button[aria-expanded=false] + ul {
    display: none;
}

.aash-dropdown-menu button[aria-expanded=true] + ul {
    display: block;
}

.aash-dropdown-menu > ul {
    list-style: none;
    position: absolute;
    z-index: var(--z-index-dropdown, 1000);
    top: 100%;
    left: 0;
    right: auto;
}`;
    document.head.appendChild(style);
  }
};

injectStyles();
