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
 * * `updateTitle(title: string): void`: update the title
 *
 * @memberof module:AashModalDialog
 */
export interface Api {
  open(): void;
  close(): void;
  cancel(): void;
  isOpen(): boolean;
  updateTitle(title: string): void;
}

let instanceCounter = 0;

/**
 * @classdesc
 * Generates a modal dialog.
 *
 * Example:
 * ```html
 * <div class="aash-modal-dialog dialog__backdrop" showcancel="true">
 *   <div id="sampleDialog" role="dialog"
 *       aria-labelledby="sampleDialog-label" aria-modal="true">
 *     <header id="sampleDialog-label">
 *       <p>Sample Dialog</p>
 *       <button type="button" class="fa fa-times"></button>
 *     </header>
 *     <section class="">
 *       <i>Sample dialog content</i>
 *     </section>
 *     <footer>
 *       <button type="button">Okay</button>
 *     </footer>
 *   </div>
 * </div>
 * ```
 * 
 * Once created, the component provides the externally invocable methods
 * defined by {@link module:AashModalDialog.Api} through an object in 
 * a property of the DOM element with `role="dialog"` (see
 * {@link module:AashUtil.getApi}). Note that this is not the mounted element
 * but the mounted element's child. It is, however, the element with the
 * provided `id`. Thus it makes more sense to use this element.
 * 
 * @class AashModalDialog
 * @param {Object} props the properties
 * @param {string} props.id the dialog's id
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
        closeLabel: {
            type: String,
            default: "Okay"
        },
        onClose: Function as PropType<(confirmed: boolean) => void>
    },
  
    setup(props, context) {
        const effectiveId = ref("");
        const effectiveTitle = ref("");
        const isOpen = ref(false);

        if (props.id) {
            effectiveId.value = props.id!;
        } else {
            effectiveId.value = "aash-modal-dialog-" + ++instanceCounter;
        }
        if (props.title) {
            effectiveTitle.value = props.title!;
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

        const updateTitle = (title: string) => {
            effectiveTitle.value = title;
        }

        const dialog = ref(null);

        provideApi(dialog, { open, close, cancel, updateTitle,
                isOpen: () => { return isOpen.value } });

        return { effectiveId, effectiveTitle, isOpen, cancel, close, dialog };
    }
});
