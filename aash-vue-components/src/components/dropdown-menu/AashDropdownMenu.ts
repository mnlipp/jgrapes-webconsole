import { defineComponent, PropType, ref, computed } from 'vue'

/**
 * Generates a dropdown menu with all required ARIA attributes.
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
        let $lastEvent: MouseEvent | null = null;
        let _globalClickHandler = (e: MouseEvent) => {};
        const expanded = ref(false);
        
        const toggle = (event: MouseEvent) => {
            $lastEvent = event;
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
    
        return { $lastEvent, _globalClickHandler, expanded, 
            toggle, translate, sortedItems };        
    },
  
    mounted() {
        // Any click event that is not the one handled by this component
        // causes the menu to be closed.
        let self = this;
        this._globalClickHandler = (event: MouseEvent) => {
            if (self.$lastEvent) {
                let lastEvent = self.$lastEvent; 
                if (event.target !== lastEvent) {
                    self.expanded = false;
                }
            }
        };
        document.addEventListener("click", this._globalClickHandler);
    },
    
    beforeDestroy() {
        document.removeEventListener("click", this._globalClickHandler);
    }
});
