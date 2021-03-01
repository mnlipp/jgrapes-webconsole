/**
 * @module AashDropdownMenu
 */
import { defineComponent, PropType, ref, computed,
    onMounted, onBeforeUnmount } from 'vue'

/**
 * @classdesc
 * A component that generates a dropdown 
 * menu with all required ARIA attributes.
 * 
 * @class
 * @param {Object} props The properties
 * @param {string} props.id The id for the enclosing `div`
 * @param {string} props.label The text of the `button` that opens the menu
 * @param {Array[]} props.items The menu items as an array
 *      of arrays with two objects, the first being the label of the menu
 *      item (a string) or a function that returns the label 
 *      and the second being an argument to the `action` function that
 *      is invoked when the item has been chosen
 * @param {function} props.l10n A function invoked with a label 
 *      (of type string)as argument before the label is rendered
 * @param {function} props.action A function that is invoked when an item has
 *      been chosen
 */
export default defineComponent({
    props: {
        id: { type: String, required: true },
        label: { type: String, required: true },
        items: { type: Array as PropType<Array<Array<any>>>, required: true },
        l10n: Function,
        action: Function
    },
    
    setup(props) {
        let lastEvent: MouseEvent | null = null;
        let globalClickHandler = (e: MouseEvent) => {};
        const expanded = ref(false);
        
        const toggle = (event: MouseEvent) => {
            lastEvent = event;
            expanded.value = !expanded.value;
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
            let result: Array<Array<any>> = [];
            for (let item of props.items) {
                result.push([translate(item[0]), item]);
            }
            result.sort(function(a, b) {
                return a[0].localeCompare(b[0]);
            });
            return result;
        });

        onMounted(() => {
            // Any click event that is not the one handled by this component
            // causes the menu to be closed.
            globalClickHandler = (event: MouseEvent) => {
                if (lastEvent) {
                    if (event.target !== lastEvent.target) {
                        expanded.value = false;
                    }
                }
            };
            document.addEventListener("click", globalClickHandler);
        });

        onBeforeUnmount(() => {
            document.removeEventListener("click", globalClickHandler);
        });
    
        return { lastEvent, globalClickHandler, expanded,
            toggle, translate, sortedItems };        
    }
});
