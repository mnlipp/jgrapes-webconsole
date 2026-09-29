/**
 * Provides modal-dialog element.
 * @module AashModalDialog
 */
import Alpine from 'alpinejs';
import {
  AashComponent,
  Attribute,
  Renderer,
  expressionConverter,
  aashId,
  createFragment
} from '../../AashUtil';

/**
 * @internal AlpineJS data definition for the modal-dialog component.
 */
interface ModalDialogData {
  hasCancelButton: boolean;
  hasApplyButton: boolean;
  hasOkayButton: boolean;
  submitForm: string | null;
  cancel: () => void;
  apply: () => void;
  close: () => void;
}

/**
 * Generates a modal dialog with all required ARIA attributes.
 *
 * The DOM is generated as shown in the
 * [WAI-ARIA Authoring Practices](https://www.w3.org/TR/wai-aria-practices-1.1/examples/dialog-modal/)
 *
 * Content is provided via <template provides="..."> children:
 *
 * ```html
 * <aash-modal-dialog id="sampleDialog"
 *     action="dialogAction(apply, close)">
 *   <template provides="dialog-title">
 *     <p>Sample Dialog</p>
 *   </template>
 *   <template provides="content">
 *     <i>Sample dialog content</i>
 *   </template>
 *   <template provides="cancel-label">
 *     <span>×</span>
 *   </template>
 *   <template provides="apply-label">
 *     <span>Apply</span>
 *   </template>
 *   <template provides="okay-label">
 *     <span>Close</span>
 *   </template>
 * </aash-modal-dialog>
 * ```
 * 
 * The rendered dialog template is:
 * 
 * ```html
 * <dialog>
 *   <header>
 *     <p>Title content</p>
 *     <button>
 *       Cancel button content
 *     </button>
 *   </header>
 *   <section>
 *     Dialog content
 *   </section>
 *   <footer>
 *     <button>
 *       Apply button content
 *     </button>
 *     <button>
 *       Okay button content
 *     </button>
 *   </footer>
 * </dialog>
 * ```
 *
 * @class AashModalDialog
 */
@AashComponent()
export class AashModalDialog extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: ModalDialogData;
  /** @internal Action callback */
  private actionCallback:
    ((apply: boolean, close: boolean) => void) | null = null;

  /**
   * Callback invoked when an action button is pressed.
   * For programmatic usage. For template usage, use the action attribute.
   * @param apply true if the apply button was pressed
   * @param close true if the close/okay button was pressed
   */
  set onAction(value: ((apply: boolean, close: boolean) => void) | null) {
    this.actionCallback = value;
  }

  /** @internal Teleport target for title area */
  private titleTarget = aashId();
  /** @internal Teleport target for cancel button label */
  private cancelLabelTarget = aashId();
  /** @internal Teleport target for apply button label */
  private applyLabelTarget = aashId();
  /** @internal Teleport target for okay button label */
  private okayLabelTarget = aashId();
  /** @internal Teleport target for content area */
  private contentTarget = aashId();
  /** @internal Observer for template children changes */
  private contentObserver = new MutationObserver(() => {
      Alpine.nextTick(() => this.updateContent());
  });
  private dialogElement: HTMLDialogElement | null = null;

  constructor() {
    super();
    const element = this;
    this.ajsData = Alpine.reactive({
      hasCancelButton: false,
      hasApplyButton: false,
      hasOkayButton: true,
      submitForm: null,

      cancel() {
        if (element.actionCallback) {
          element.actionCallback(false, false);
        }
        element.dialogElement?.close();
        element.ownerDocument.querySelector('html')!.removeAttribute('inert');
      },

      apply() {
        if (element.actionCallback) {
          element.actionCallback(true, false);
        }
      },

      close() {
        element.close();
      }
    } satisfies ModalDialogData);
  }

  /** @internal Renders dialog structure and teleports content. */
  @Renderer()
  protected render() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    const dialogId = this.id || `aash-modal-dialog`;
    const labelId = dialogId + '-label';
    this.dialogElement = createFragment(this,
      `<dialog class="aash-modal-dialog dialog__backdrop" id="${dialogId}"
        role="dialog" aria-modal="true" aria-labelledby="${labelId}" x-cloak>
        <header id="${labelId}">
          <p id="${this.titleTarget}"></p>
          <button x-show="hasCancelButton" type="button"
              id="${this.cancelLabelTarget}" @click="cancel()">
          </button>
        </header>
        <section id="${this.contentTarget}">
        </section>
        <footer x-show="hasApplyButton || hasOkayButton">
          <button id="${this.applyLabelTarget}"
              x-show="hasApplyButton"
              x-bind:form="submitForm"
              x-bind:type="submitForm ? 'submit' : 'button'"
              @click="apply()">
          </button>
          <button id="${this.okayLabelTarget}"
              x-show="hasOkayButton"
              x-bind:form="submitForm"
              x-bind:type="submitForm ? 'submit' : 'button'"
              @click="close()">
          </button>
        </footer>
      </dialog>`).firstChild as HTMLDialogElement;
    this.prepend(this.dialogElement!);
    // Let the new DOM settle.
    Alpine.nextTick(() => this.updateContent());
  }

  /** @internal Teleports template content into structural targets. */
  private updateContent() {
    this.contentObserver.disconnect();
    const titleSpan = this.querySelector(`#${this.titleTarget}`)!;
    const titleTemplate
      = this.querySelector(':scope > [provides="dialog-title"]');
    if (titleTemplate && !titleTemplate.hasAttribute("x-teleport")) {
      titleSpan.textContent = '';
      titleTemplate.setAttribute("x-teleport", "#" + this.titleTarget);
    }
    const cancelButton = this.querySelector(`#${this.cancelLabelTarget}`)!;
    const cancelLabelTemplate
      = this.querySelector(':scope > [provides="cancel-label"]');
    if (cancelLabelTemplate) {
      if (!cancelLabelTemplate.hasAttribute("x-teleport")) {
        cancelLabelTemplate.setAttribute(
          "x-teleport", "#" + this.cancelLabelTarget);
      }
      this.ajsData.hasCancelButton = true;
    } else {
      this.ajsData.hasCancelButton = false;
    }
    const applyButton = this.querySelector(`#${this.applyLabelTarget}`)!;
    const applyLabelTemplate
      = this.querySelector(':scope > [provides="apply-label"]');
    if (applyLabelTemplate) {
      if (!applyLabelTemplate.hasAttribute("x-teleport")) {
        applyLabelTemplate.setAttribute(
            "x-teleport", "#" + this.applyLabelTarget);
      }
      this.ajsData.hasApplyButton = true;
    } else {
      this.ajsData.hasApplyButton = false;
    }
    const okayButton = this.querySelector(`#${this.okayLabelTarget}`)!;
    const okayLabelTemplate = this.querySelector(':scope > [provides="okay-label"]');
    if (okayLabelTemplate) {
      if (!okayLabelTemplate.hasAttribute("x-teleport")) {
        okayLabelTemplate.setAttribute("x-teleport", "#" + this.okayLabelTarget);
      }
      this.ajsData.hasOkayButton = true;
    } else {
      this.ajsData.hasOkayButton = true;
      okayButton.textContent = 'OK';
    }
    const contentSpan = this.querySelector(`#${this.contentTarget}`)!;
    const contentTemplate = this.querySelector(':scope > [provides="content"]');
    if (contentTemplate && !contentTemplate.hasAttribute("x-teleport")) {
      contentSpan.textContent = '';
      contentTemplate.setAttribute("x-teleport", "#" + this.contentTarget);
    }
    this.contentObserver.observe(this, {
      childList: true,
      subtree: false,
      attributes: false,
      characterData: false,
    });
  }

  disconnectedCallback() {
    this.contentObserver.disconnect();
  }

  /**
   * Opens the dialog.
   */
  open(): void {
    this.ownerDocument.querySelector('html')!.setAttribute('inert', '');
    // Maybe not rendered yet
    if (!!this.dialogElement) {
       this.dialogElement!.showModal();
    } else {
        Alpine.nextTick(() => this.dialogElement?.showModal());
    }
  }

  /**
   * Closes the dialog (confirm action).
   */
  close(): void {
    if (this.actionCallback) {
      this.actionCallback(true, true);
    }
    this.dialogElement?.close();
    this.ownerDocument.querySelector('html')!.removeAttribute('inert');
  }

  /**
   * Cancels the dialog.
   */
  cancel(): void {
    this.ajsData.cancel();
  }

  /**
   * ID of a form to associate with action buttons.
   */
  @Attribute('submit-form')
  set submitForm(value: string | null) {
    this.ajsData.submitForm = value;
  }

  /**
   * The action callback invoked when an action button is pressed.
   * Receives (apply: boolean, close: boolean) as arguments.
   */
  @Attribute('action', expressionConverter)
  set action(value: ((apply: boolean, close: boolean) => void) | null) {
    this.actionCallback = value;
  }
}

customElements.define('aash-modal-dialog', AashModalDialog);

/**
 * Injects the global styles for the dialog once.
 */
const injectStyles = () => {
  if (!document.querySelector('style[aash-modal-dialog-styles]')) {
    const style = document.createElement('style');
    style.setAttribute('aash-modal-dialog-styles', '');
    style.textContent = `
dialog.aash-modal-dialog header {
  display: flex;
}

dialog.aash-modal-dialog header > :first-child {
  flex-grow: 1;
}

dialog.aash-modal-dialog footer {
  display: flex;
  justify-content: end;
}`;
    document.head.appendChild(style);
  }
};

injectStyles();

