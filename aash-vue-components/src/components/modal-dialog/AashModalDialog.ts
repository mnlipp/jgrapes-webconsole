/**
 * @module AashModalDialog
 */
import { defineComponent, PropType, ref, computed, onMounted, watch } from 'vue'

/**
 * The interface provided by the component.
 *
 * * `open(): void`: opens the dialog
 * * `close(): void`: closes the dialog
 * * `cancel(): void`: cancels the dialog
 * * `isOpen(): boolean`: returns the state of the dialog
 *
 * @memberOf module:AashModalDialog
 */
export interface AashApi {
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

        onMounted(() => {
            let api: AashApi = { open, close, cancel, 
                isOpen: () => { return isOpen.value } };
            if (dialog.value) {
                (<any>(dialog.value!)).__aashApi = api;
            }
        });

        return { effectiveId, isOpen, cancel, close, dialog };
    }
});
