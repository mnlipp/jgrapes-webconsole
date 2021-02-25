import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

@Component
/**
 * Generates a dropdown menu with all required ARIA attributes.
 */
export default class AashDropdownMenu extends Vue {
    // The id attribute of the generated top level HTML element 
    @Prop({ type: String, required: true }) readonly id!: string;
    // The text of the generated `button` element that opens the menu
    @Prop({ type: String, required: true }) readonly label!: string;
    // The menu items as an array
    // of arrays with two objects, the first being the label of the menu
    // item (a string) or a function that returns the label 
    // and the second being an argument to the `action` function that
    // is invoked when the item has been chosen.
    @Prop({ type: Array, required: true }) items!: Array<Array<any>>;
    // A function invoked with a label 
    // (of type string)as argument before the label is rendered
    @Prop({ type: Function }) l10n: any;
    // A function that is invoked when an item has been chosen
    @Prop({ type: Function }) action: any;

    private _globalClickHandler = (e: MouseEvent) => {};
    $lastEvent: any = null;
    expanded: boolean = false;

    /**
     * Toggle the menu's visibility.
     *
     * @vuese
     * @arg The mouse event
     */
    toggle(event: MouseEvent) {
        this.$lastEvent = event;
        this.expanded = !this.expanded;
    }
  
    private translate(raw: any) {
        if (typeof(raw) === "function") {
            return raw();
        }
        if (this.l10n) {
            return this.l10n(raw);
        }
        return raw;
    }

    get sortedItems() {
        let result = [];
        for (let item of this.items) {
            result.push([this.translate(item[0]), item]);
        }
        result.sort(function(a, b) {
            return a[0].localeCompare(b[0]);
        });
        return result;
    }    
    
    mounted() {
        // Any click event that is not the one handled by this component
        // causes the menu to be closed.
        let self = this;
        this._globalClickHandler = function(event: MouseEvent) {
            if (self.$lastEvent && event.target !== self.$lastEvent.target) {
                self.expanded = false;
            }
        };
        document.addEventListener("click", this._globalClickHandler);
    }
    
    beforeDestroy() {
        document.removeEventListener("click", this._globalClickHandler);
    }
}
