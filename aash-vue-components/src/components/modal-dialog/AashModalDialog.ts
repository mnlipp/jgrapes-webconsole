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
 * <div id="sampleDialog" class="aash-modal-dialog dialog__backdrop"
 *     showcancel="true">
 *   <div role="dialog"
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
 * a property of the mounted element (which also has the provided `id`).
 * 
 * By default, the button is generated with type `button`. If your dialog
 * contains a form and you want to make use of the browser's support for
 * autocompletion, the button must have type `submit` (although submitting
 * will be prevented). You can force this by setting `submitForm` to the
 * `id` of the form in the dialog content.
 *
 * The dialog is generated with an apply button and an okay (confirm)
 * button. The latter closes the form when pushed. The buttons are
 * generate when the respective labels aren't empty. If both labels
 * are empty, there will be no footer. This can be used for a purely
 * informative dialog that has only a "cancel" (close) element.
 *
 * @class AashModalDialog
 * @param {Object} props the properties
 * @param {string} props.id the dialog's id
 * @param {string} props.title the dialog's title
 * @param {boolean} props.showCancel whether to show a cancel button
 * @param {string} props.content the dialog's content
 * @param {Array.string} props.contentClasses classes to apply to the content
 * @param {string} props.applyLabel the label for the apply button; defaults
 * to empty
 * @param {string} props.okayLabel the label for the okay (confirm) button;
 * defaults to "OK"
 * @param {Function} props.onAction the function to invoke on action
 * @param {string} props.submitForm the id of a form
 */
export default defineComponent({
    props: {
        id: String,
        title: String,
        showCancel: { type: Boolean, default: true },
        content: String,
        contentClasses: Array,
        applyLabel: { type: String, default: "" },
        okayLabel: { type: String, default: "OK" },
        onAction: Function as PropType<(apply: boolean, 
            close: boolean) => void>,
        submitForm: { type: String, default: null }
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
            if (props.onAction) {
                props.onAction(false, false);
            }
            isOpen.value = false;
        }

        const apply = () => {
            if (props.onAction) {
                props.onAction(true, false);
            }
        }

        const close = () => {
            if (props.onAction) {
                props.onAction(true, true);
            }
            isOpen.value = false;
        }

        const updateTitle = (title: string) => {
            effectiveTitle.value = title;
        }

        const noSubmit = (e: Event) => {
            e.preventDefault();
        }

        const dialog = ref(null);

        provideApi(dialog, { open, apply, close, cancel, updateTitle,
                isOpen: () => { return isOpen.value } });

        return { effectiveId, effectiveTitle, isOpen, apply, 
            cancel, close, dialog, noSubmit };
    }
});
