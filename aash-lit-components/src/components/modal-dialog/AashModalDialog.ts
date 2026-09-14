/**
 * Provides a modal dialog.
 * @module AashModalDialog
 */
import { LitElement, html, nothing } from 'lit';
import { property, state } from 'lit/decorators.js';

let instanceCounter = 0;

/**
 * Generates a modal dialog.
 *
 * Example:
 * ```html
 * <aash-modal-dialog id="sampleDialog"
 *     dialog-title="Sample Dialog"
 *     show-cancel="true">
 *   <i>Sample dialog content</i>
 * </aash-modal-dialog>
 * ```
 *
 * Once created, the component exposes the methods
 * `open()`, `close()`, `cancel()`, and `updateTitle(title)` and
 * the property `isOpen` directly on the element instance.
 *
 * By default, the button is generated with type `button`. If your dialog
 * contains a form and you want to make use of the browser's support for
 * autocompletion, the button must have type `submit` (although submitting
 * will be prevented). You can force this by setting `submit-form` to the
 * `id` of the form in the dialog content.
 *
 * The dialog is generated with an apply button and an okay (confirm)
 * button. The latter closes the form when pushed. The buttons are
 * generate when the respective labels aren't empty. If both labels
 * are empty, there will be no footer. This can be used for a purely
 * informative dialog that has only a "cancel" (close) element.
 *
 * @class AashModalDialog
 */
export class AashModalDialog extends LitElement {

  /** The dialog's title */
  @property({ type: String })
  public dialogTitle: string = '';

  /** Whether to show a cancel button */
  @property({ type: Boolean, attribute: 'show-cancel' })
  public showCancel: boolean = true;

  /** Classes to apply to the content section */
  @property({ type: Array, attribute: 'content-classes' })
  public contentClasses: string[] = [];

  /**
   * The label for the apply button.
   * Defaults to empty (no apply button).
   */
  @property({ type: String, attribute: 'apply-label' })
  public applyLabel: string = '';

  /**
   * The label for the okay (confirm) button.
   * Defaults to "OK".
   */
  @property({ type: String, attribute: 'okay-label' })
  public okayLabel: string = 'OK';

  /**
   * The function to invoke on action.
   * Called with (apply: boolean, close: boolean).
   */
  @property({ attribute: 'on-action', converter: { fromAttribute: () => null } })
  public onAction: ((apply: boolean, close: boolean) => void) | null = null;

  /** The id of a form to submit */
  @property({ type: String, attribute: 'submit-form' })
  public submitForm: string | null = null;

  /** Whether the dialog is currently open */
  @state()
  public isOpen: boolean = false;

  private content: Element[] = [];

  private effectiveId: string = '';

  @state()
  private effectiveTitle: string = '';

  private get dialogElement(): HTMLDialogElement | null {
    return this.querySelector('dialog');
  }

  protected override createRenderRoot() {
    return this as any;
  }

  override connectedCallback(): void {
    this.content = Array.from(this.children);
    super.connectedCallback();
    if (this.id) {
      this.effectiveId = this.id;
    } else {
      this.effectiveId = `aash-modal-dialog-${++instanceCounter}`;
      this.id = this.effectiveId;
    }
    this.effectiveTitle = this.dialogTitle;
  }

  override willUpdate(changedProperties: Map<string, any>): void {
    super.willUpdate(changedProperties);
    if (changedProperties.has('dialogTitle')) {
      this.effectiveTitle = this.dialogTitle;
    }
  }

  /** Opens the dialog */
  public async open(): Promise<void> {
    await this.updateComplete;
    if (this.dialogElement) {
      this.dialogElement.showModal();
      this.isOpen = true;
    }
  }

  /** Cancels the dialog (invokes onAction with (false, false)) */
  public cancel(): void {
    if (this.onAction) {
      this.onAction(false, false);
    }
    if (this.dialogElement) {
      this.dialogElement.close();
    }
    this.isOpen = false;
  }

  /** Applies (invokes onAction with (true, false)) */
  private apply(): void {
    if (this.onAction) {
      this.onAction(true, false);
    }
  }

  /** Closes the dialog (invokes onAction with (true, true)) */
  public close(): void {
    if (this.onAction) {
      this.onAction(true, true);
    }
    if (this.dialogElement) {
      this.dialogElement.close();
    }
    this.isOpen = false;
  }

  /** Updates the dialog title */
  public updateTitle(title: string): void {
    this.effectiveTitle = title;
  }

  private noSubmit(e: Event): void {
    e.preventDefault();
  }

  private onDialogClose(): void {
    this.isOpen = false;
  }

  private onDialogCancel(): void {
    if (this.onAction) {
      this.onAction(false, false);
    }
    this.isOpen = false;
  }

  private get buttonType(): string {
    return this.submitForm ? 'submit' : 'button';
  }

  protected override render() {
    const labelId = `${this.effectiveId}-label`;

    const contentSection = html`<section
          class=${this.contentClasses.join(' ') || nothing}
          >${this.content}</section>`;

    const hasFooter = this.applyLabel !== '' || this.okayLabel !== '';

    return html`
      <dialog id="${this.effectiveId}"
          class="aash-modal-dialog"
          aria-labelledby="${labelId}"
          aria-modal="true"
          @close=${this.onDialogClose}
          @cancel=${this.onDialogCancel}>
        <header id="${labelId}">
          <p>${this.effectiveTitle}</p>
          ${this.showCancel
            ? html`<button type="button" class="fa fa-times"
                  @click=${() => this.cancel()}></button>`
            : nothing}
        </header>
        ${contentSection}
        ${hasFooter ? html`
          <footer>
            ${this.applyLabel !== ''
              ? html`<button type="${this.buttonType}"
                    form=${this.submitForm || nothing}
                    @click=${() => this.apply()}>${this.applyLabel}</button>`
              : nothing}
            ${this.okayLabel !== ''
              ? html`<button type="${this.buttonType}"
                    form=${this.submitForm || nothing}
                    @submit=${this.noSubmit}
                    @click=${() => this.close()}>${this.okayLabel}</button>`
              : nothing}
          </footer>
        ` : nothing}
      </dialog>
    `;
  }
}

customElements.define('aash-modal-dialog', AashModalDialog);
