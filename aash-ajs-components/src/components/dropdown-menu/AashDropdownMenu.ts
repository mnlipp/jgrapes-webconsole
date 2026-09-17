/**
 * Provides dropdown-menu element.
 * @module AashDropdownMenu
 */
import Alpine from 'alpinejs';
import {
  AashComponent,
  Attribute,
  functionConverter
} from '../../AashUtil';

/**
 * A single menu item consisting of a label and an opaque value.
 * The label is either a string or a function returning a string.
 */
export type MenuItem = [string | (() => string), any];

/**
 * @internal AlpineJS data definition for the dropdown-menu component.
 */
interface DropdownMenuData {
  expanded: boolean;
  label: string;
  items: Array<[string, MenuItem]>;
  l10n: ((key: string) => string) | null;
  action: ((item: MenuItem) => void) | null;
  toggle: (event: MouseEvent) => void;
  chooseItem: (item: MenuItem) => void;
  close: () => void;
  labelItem: (item: [string, MenuItem]) => string;
}

/**
 * Generates a dropdown menu with all required ARIA attributes.
 *
 * The DOM is generated as shown in the
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/menu-button/menu-button-actions.html)
 *
 * Example:
 * ```html
 * <aash-dropdown-menu id="language-selector"
 *     label="Language"></aash-dropdown-menu>
 * <script>
 *   const menu = document.getElementById('language-selector');
 *   menu.items = [
 *     ['English', 'English chosen'],
 *     ['German', 'German chosen']
 *   ];
 *   menu.action = function(item) {
 *     window.alert(item[1]);
 *   };
 * </script>
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
  private lastEvent: MouseEvent | null = null;

  constructor() {
    super();
    this.ajsData = Alpine.reactive(this.dropdownMenuAlpineData());
  }

  /**
   * @internal AlpineJS data definition for the dropdown-menu component.
   */
  protected dropdownMenuAlpineData(): DropdownMenuData {
    const element = this;

    return {
      expanded: false,
      label: '',
      items: [] as Array<[string, MenuItem]>,
      action: null,
      l10n: null,

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

      chooseItem(item: MenuItem) {
        if (this.action) {
          this.action(item);
        }
        this.close();
      },

      close() {
        this.expanded = false;
        element.removeGlobalClickHandler();
      },

      labelItem(item: [string, MenuItem]): string {
        return item[0];
      }
    };
  }

  /** @internal Removes the global click handler */
  protected removeGlobalClickHandler() {
    if (this.globalClickHandler) {
      document.removeEventListener(
        "click", this.globalClickHandler);
      this.globalClickHandler = null;
    }
  }

  protected connectedCallback() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    this.render();
    Alpine.initTree(this);
  }

  /** @internal Renders dropdown as HTML with Alpine directives. */
  protected render() {
    const menuId = this.id + '-menu';
    this.innerHTML =
      `<div class="dropdown-menu aash-dropdown-menu" x-cloak>
        <button type="button" aria-haspopup="menu"
            x-bind:aria-controls="'${menuId}'"
            x-bind:aria-expanded="expanded
                ? 'true' : 'false'" @click="toggle">
            <span x-html="label"></span>
        </button>
        <ul x-bind:id="'${menuId}'" role="menu">
            <template x-for="item of items">
                <li role="none">
                    <button type="button" role="menuitem"
                        @click="chooseItem(item[1])"
                        x-text="labelItem(item)">
                    </button>
                </li>
            </template>
        </ul>
    </div>`;
  }

  /**
   * The menu items as an array of MenuItem tuples.
   * Each item is `[label | () => label, value]`.
   * Items are sorted alphabetically by (translated) label.
   */
  set items(value: MenuItem[]) {
    const l10n = this.ajsData.l10n;

    const translate = (raw: string | (() => string)): string => {
      const resolved = typeof raw === 'function' ? raw() : raw;
      if (l10n) {
        return l10n(resolved);
      }
      return resolved;
    };

    this.ajsData.items = value.map(item => [
      translate(item[0]), item
    ]);
    this.ajsData.items.sort(
      (a, b) => a[0].localeCompare(b[0]));
  }

  /**
   * The text of the button that opens the menu.
   */
  @Attribute('label')
  set label(value: string) {
    this.ajsData.label = value;
  }

  /**
   * The action callback invoked when a menu item is chosen.
   * Receives the chosen MenuItem as argument.
   */
  @Attribute('action', functionConverter)
  set action(value: ((item: MenuItem) => void) | null) {
    this.ajsData.action = value;
  }

  /**
   * Sets the localization function applied to menu item labels
   * before rendering.
   */
  @Attribute('l10n', functionConverter)
  set l10n(value: ((key: string) => string) | null) {
    this.ajsData.l10n = value;
  }

  disconnectedCallback() {
    this.removeGlobalClickHandler();
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
