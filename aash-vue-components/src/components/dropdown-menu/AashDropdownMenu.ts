/**
 * @module AashDropdownMenu
 */
import { defineComponent, PropType, ref, computed,
    onMounted, onBeforeUnmount } from 'vue'

type MenuItem = [ string | (() => string), any ];

/**
 * @classdesc
 * A component that generates a dropdown menu with all required ARIA 
 * attributes.
 * 
 * The DOM is generated as shown in the 
 * [WAI-ARIA Authoring Practices 1.1](https://www.w3.org/TR/wai-aria-practices-1.1/examples/menu-button/menu-button-actions.html)
 *
 * Example:
 * ```html 
 * <div id="language-selector" class="aash-dropdown-menu">
 *  <button type="button" aria-haspopup="menu"
 *   aria-controls="language-selector-menu" aria-expanded="true">
 *   <span>Language</span>
 *  </button>
 *  <ul id="language-selector-menu" role="menu">
 *   <li role="none">
 *    <button type="button" role="menuitem">English</button>
 *   </li>
 *   <li role="none">
 *    <button type="button" role="menuitem">German</button>
 *   </li>
 *  </ul>
 * </div>
 * ```
 * 
 * @class
 * @param {Object} props The properties
 * @param {string} props.id The id for the enclosing `div`
 * @param {string} props.label The text of the `button` that opens the menu
 * @param {Array} props.items The menu items as an array with elements
 *      of type `MenuItem`
 *      (`[ string | (() => string), any ]`). Each item consists 
 *      of two elements, the first being the label of the menu
 *      item (a string) or a function that returns the label 
 *      and the second being an opaque value to forward information to the 
 *      `action` function that is invoked when the item has been chosen
 * @param {function} props.l10n A function invoked with a label 
 *      (of type string) as argument before the label is rendered
 * @param {function} props.action A function that is invoked when an item has
 *      been chosen. The chosen item, i.e. the entry from the list of
 *      `items` is passed as an argument.
 */
export default defineComponent({
    props: {
        id: { type: String, required: true },
        label: { type: String, required: true },
        items: { type: Array as PropType<Array<MenuItem>>, required: true },
        l10n: Function as PropType<(key: string) => string>,
        action: Function as PropType<(arg: MenuItem) => void>
    },
    
    setup(props) {
        let lastEvent: MouseEvent | null = null;
        const expanded = ref(false);
        // Any click event that is not the one handled by this component
        // causes the menu to be closed.
        const globalClickHandler = (event: MouseEvent) => {
            if (lastEvent) {
                if (event.target !== lastEvent.target) {
                    expanded.value = false;
                }
            }
            if (!expanded.value) {
                document.removeEventListener("click", globalClickHandler);
            }
        };
        
        const toggle = (event: MouseEvent) => {
            lastEvent = event;
            expanded.value = !expanded.value;
            if (expanded.value) {
                document.addEventListener("click", globalClickHandler);
            } else {
                document.removeEventListener("click", globalClickHandler);
            }
        }

        const translate = (raw: string | (() => string)) => {
            if (typeof(raw) === "function") {
                return raw();
            }
            if (props.l10n) {
                return props.l10n(raw);
            }
            return raw;
        }
        
        const sortedItems = computed(() => {
            let result: Array<[string, MenuItem]> = [];
            for (let item of props.items) {
                result.push([translate(item[0]), item]);
            }
            result.sort(function(a, b) {
                return a[0].localeCompare(b[0]);
            });
            return result;
        });

        onBeforeUnmount(() => {
            if (globalClickHandler) {
                document.removeEventListener("click", globalClickHandler);
            }
        });
    
        return { lastEvent, globalClickHandler, expanded,
            toggle, translate, sortedItems };        
    }
});
