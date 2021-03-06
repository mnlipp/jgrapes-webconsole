/**
 * Provides a modal dialog. 
 * @module AashModalDialog
 */
import { defineComponent, PropType, ref, computed, onMounted, watch } from 'vue'
import { provideApi } from "../../AashUtil";

/**
 * The interface provided by the component.
 *
 * * `open(): void`: opens the dialog
 * * `close(): void`: closes the dialog
 * * `cancel(): void`: cancels the dialog
 * * `isOpen(): boolean`: returns the state of the dialog
 *
 * @memberof module:AashModalDialog
 */
export interface Api {
  open(): void;
  close(): void;
  cancel(): void;
  isOpen(): boolean;
}

let instanceCounter = 0;

/**
 * @classdesc
 * Generates a ... 
 * 
 * Once created, the component provides the externally invocable methods
 * defined by {@link module:AashTablist.Api} through an object in 
 * a property of the mounted DOM element {@link module:AashUtil.getApi}.
 * 
 * @class AashModalDialog
 * @param {Object} props the properties
 * @param {string} props.title the dialog's title
 * @param {boolean} props.showCancel wether to show a cancel button
 * @param {string} props.content the dialog's content
 * @param {Array.string} props.contentClasses classes to apply to the content
 * @param {string} props.closeLabel the label for the close button
 * @param {Function} props.onClose the function to invoke on close
 */
export default defineComponent({
    props: {
        id: String,
        title: String,
        showCancel: {
            type: Boolean,
            default: true
        },
        content: String,
        contentClasses: Array,
        closeLabel: String,
        onClose: Function as PropType<(confirmed: boolean) => void>
    },
  
    setup(props, context) {
        const effectiveId = ref("");
        const isOpen = ref(false);

        if (props.id) {
            effectiveId.value = props.id!;
        } else {
            effectiveId.value = "aash-modal-dialog-" + ++instanceCounter;
        }
        
        const open = () => {
            isOpen.value = true;
        }

        const cancel = () => {
            if (props.onClose) {
                props.onClose(false);
            }
            isOpen.value = false;
        }
  
        const close = () => {
            if (props.onClose) {
                props.onClose(true);
            }
            isOpen.value = false;
        }

        const dialog = ref(null);

        provideApi(dialog, { open, close, cancel, 
                isOpen: () => { return isOpen.value } });

        return { effectiveId, isOpen, cancel, close, dialog };
    }
});
