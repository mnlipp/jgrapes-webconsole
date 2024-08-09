/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2022  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
import Console, { PageComponentSpecification } from "./Console";
import Log from "./Log";
import ConsoleNotification from "./Notification";
import NotificationOptions from "./NotificationOptions";
import ModalDialogOptions from "./ModalDialogOptions";
import RenderMode from "./RenderMode";
import { parseHtml } from "./Util";

/**
 * Represents a conlet in the DOM tree. Often, this is backed by
 * a `div` that serves as a container for the conlet representation.
 * Especially when providing content, however, there may not be such
 * a wrapper element.
 *
 * Note: it would have been more precise to call this class 
 * `ConletRepresentation`. But as representations of conlets are all we 
 * have in this context, the shorter name was chosen for the sake of
 * brevity.
 * 
 */
abstract class Conlet {
    /**
     * The conlet id.
     * @return the conlet id
     */
    abstract id(): string;

    /**
     * The root element of the conlet representation.
     * @return the element from the DOM
     */    
    abstract element(): HTMLElement;
    
    /**
     * True, if this represents a preview.
     */
    abstract isPreview(): boolean;
    
    /**
     * True, if this represents a view.
     */
    abstract isView(): boolean;
    
    /**
     * True, if this represents content.
     */
    abstract isContent(): boolean;
}

/**
 * A default implementation ov {@link Conlet} that assumes that
 * the conlet representation in the DOM is an element with class
 * `conlet` and a dataset attribute `conlet-id`. Different kinds
 * of conlet representationa are distinguished by additional classes
 * `conlet-preview`, `conlet-view` and `conlet-content`. 
 */
class DefaultConlet extends Conlet {
    
    private _element: HTMLElement;
    
    constructor(element: HTMLElement) {
        super();
        this._element = element;
    }
    
    id() {
        return this._element.dataset["conletId"]!;
    }
    
    element() {
        return this._element;
    }
    
    isPreview() {
        return this._element.classList.contains("conlet-preview");
    }
    
    isView() {
        return this._element.classList.contains("conlet-view");
    }
    
    isContent() {
        return this._element.classList.contains("conlet-content");
    }
}


/**
 * A base class for implementing a web console renderer. The renderer
 * creates the DOM for the SPA, usually based on some initial DOM from 
 * the console page. It also provides methods for the management of the 
 * conlets' representations in the DOM tree.
 */
abstract class Renderer {

    private _console: Console;

    /** Creates a new renderer and attaches it to the console. */
    constructor(console: Console) {
        this._console = console;
        console.renderer = this;
    }

    /**
     * Called from {@link Console.init} with the options passed to
     * {@link Console.init}.
     *
     * @param options the options
     */
    init(options: any) {
    }

    /**
     * Returns the console object passed to the constructor.
     */
    protected get console() {
        return this._console;
    }

    /**
     * Wraps a node (HTMLElement) in a {@link Conlet}s.
     * If the argumnet is `null`, returns `null`.
     */
    protected wrapConletNode(node: HTMLElement | null): Conlet | null {
        if (!node) {
            return null;
        }
        return new DefaultConlet(node);
    }

    /**
     * Wraps a node list of HTMLElements in {@link Conlet}s.
     */
    protected wrapConletNodes(nodes: NodeListOf<HTMLElement>): Conlet[] {
        let result: Conlet[] = [];
        nodes.forEach(node => result.push(new DefaultConlet(node)));
        return result;
    }

    /**
     * Called by the console instance when the connection to the server is 
     * lost. The default implementation logs a warning message.  
     * Should be overridden by a method that displays a notification.
     */
    connectionLost() {
        Log.warn("Connection lost notification not implemented!");
    }

    /**
     * Called from the console when the connection to the server is restored.
     * The default implementation logs a warning message.
     * Should be overridden by a method that displays a notification.
     */
    connectionRestored() {
        Log.warn("Connection restored notification not implemented!");
    }

    /**
     * Called from the console when the connection to the server is 
     * suspended. The default implementation logs a warning message.
     * Should be overridden by a method that displays a modal dialog.
     * 
     * The connection can be resumed by invoking the function passed
     * as argument.
     * 
     * @param resume called when resuming
     */
    connectionSuspended(resume: () => void) {
        Log.warn("Connection suspended dialog not implemented!");
    }

    /**
     * Called from the console when the `consoleConfigured` notification
     * is received from the server.
     *
     * The default implementation logs a warning message.
     * Should be overridden by a method that displays a modal dialog.
     */
    consoleConfigured() {
        Log.warn("Console configured handling not implemented!");
    }

    /**
     * Called from the {@link Console} when a new conlet type is added.
     *
     * @param conletType the conlet type
     * @param displayNames the display names for the conlet type by lang
     * @param renderModes the render modes
     * @param pageComponents of components to be added to the page
     */
    addConletType(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[], 
            pageComponents: PageComponentSpecification[]) {
        Log.warn("Not implemented!");
    }

    /**
     * Called from the {@link Console} when a conlet type is removed.
     *
     * The expected action of the renderer is to remove the 
     * conlet type from e.g. a menu that allows adding conlets.
     * However, front-ends can also force a reload of the application.
     *
     * @param conletType the conlet type
     * @param renderModes the render modes
     */
    updateConletType(conletType: string, renderModes: RenderMode[]) {
        Log.warn("Not implemented!");
    }

    /**
     * Called from the {@link Console} when the console layout is received
     * from the server.
     *
     * @param previewLayout the conlet ids from top left
     * to bottom right
     * @param tabsLayout the ids of the conlets viewable in tabs
     * @param xtraInfo extra information spcific to the 
     * console implementation
     */
    lastConsoleLayout(previewLayout: string[], tabsLayout: string[],
            xtraInfo: Object) {
        Log.warn("Not implemented!");
    }

    /**
     * Called from the {@link Console} to update a conlet that is used
     * to provide content. The default implementation removes all children
     * from the container and inserts the new content.
     *
     * @param conlet the conlet as returned
     * from e.g. {@link Renderer.findConletContents}
     * @param content the component content as DOM received from the server,
     * usually inserted into the container
     */
    updateConletContent(conlet: Conlet, content: HTMLElement[]) {
        let _this = this;
        while (conlet.element().firstChild) {
            conlet.element().removeChild(conlet.element().lastChild!);
        }
        conlet.element().append(...content);
    }

    /**
     * Called from the {@link Console} to update the preview of 
     * the given conlet. If the preview is new (no preview with the
     * given conlet id exists), the console provides DOM for the
     * container as a convenience. Implementations of this method
     * are free to extract the data from the `container` argument 
     * and provide their own markup for a new container. 
     *
     * @param isNew `true` if it is a new (not yet existing) conlet preview
     * @param conlet the container for the preview representation. Either the 
     * result from {@link Renderer.findConletPreview}, if an existing preview
     * is updated, or a prepared container for the preview,
     * provided as:
     * ```
     * <section class='conlet conlet-preview' 
     *      data-conlet-type='...' data-conlet-id='...' 
     *      data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>
     * ```
     * @param modes the supported conlet modes
     * @param content the preview content as DOM received from the server,
     * usually inserted into the container
     * @param foreground `true` if the preview (i.e. the overview
     * plane) is to be made the active tab
     */
    updateConletPreview(isNew: boolean, conlet: Conlet, 
        modes: RenderMode[], content: HTMLElement[], foreground: boolean) {
        Log.warn("Not implemented!");
    }

    /**
     * Called from the {@link Console} to update the view of the given conlet.
     * If the view is new (no view with the
     * given conlet id exists), the console provides DOM for the
     * container as a convenience. Implementations of this method
     * are free to extract the data from the `conatiner` argument and 
     * provide their own markup for a new container
     *
     * @param isNew `true` if it is a new conlet view
     * @param conlet the container for the view representation. Either the 
     * result from {@link Renderer.findConletView}, if an existing preview
     * is updated, or a new container provided as:
     * ```
     * <article class="conlet conlet-view
     *      data-conlet-type='...' data-conlet-id='...'"></article>"
     * ```
     * @param modes the supported conlet modes
     * @param content the view content, usually inserted into the container
     * @param foreground `true` if the view 
     * is to be made the active tab
     */
    updateConletView(isNew: boolean, conlet: Conlet, 
        modes: RenderMode[], content: HTMLElement[], foreground: boolean) {
        Log.warn("Not implemented!");
    }

    /**
     * Called by the {@link Console} to remove the given conlet 
     * representations, which may be 
     * preview or view containers, from the DOM.
     *
     * @param conlets the conlets to remove 
     */
    removeConletDisplays(conlets: Conlet[]) {
        Log.warn("Not implemented!");
    }

    /**
     * Called bythe {@link Console} to update the title of the 
     * conlet with the given id.
     *
     * @param conletId the conlet id
     * @param title the new title
     */
    updateConletTitle(conletId: string, title: string) {
        Log.warn("Not implemented!");
    }

    /**
     * Called by the {@link Console} to update the modes of the 
     * conlet with the given id.
     * 
     * @param conletId the conlet id
     * @param modes the modes
     */
    updateConletModes(conletId: string, modes: RenderMode[]) {
        Log.warn("Not implemented!");
    }

    /**
     * Called by the {@link Console} to open a modal dialog.
     *
     * The container has a generated dialog id and attributes
     * `data-conlet-type` and `data-conlet-id`. If 
     * {@link ModalDialogOptions.useSubmit} is `true`, the submit button
     * generated by the console must be linked to the `form` in the
     * content. By convention, this is done using the dialog's `id`
     * with "-form" appended (i.e. used as the form's `id` ans the
     * button's `form` attribute).
     * 
     * @param container the container for the dialog
     * @param options the options
     * @param content the content as HTML
     */
    abstract openModalDialog(container: HTMLElement, 
        options: ModalDialogOptions, content: string): void;

    /**
     * Called by the {@link Console} to close a modal dialog
     * when a corrsponding request is received from the server.
     * 
     * @param container the container for the dialog
     */
    abstract closeModalDialog(container: HTMLElement): void;

    /**
     * Called by the {@link Console} to displays a notification.
     *
     * @param content the content to display
     * @param options the options
     */
    notification(content: string, options: NotificationOptions = {})
            : ConsoleNotification {
        Log.warn("Not implemented!");
        return  {
            close: () => {}
        }
    }

    // Utility methods.

    /**
     * Find the conlets that display the preview, view or content of 
     * the conlet with the given id. The default implementation wraps
     * all nodes with `.conlet[data-conlet-id='<conletId>'` in
     * {@link DefaultConlet}s.
     * 
     * @param conletId the conlet id
     * @return the conlets found
     */
    findConlets(conletId: string): Conlet[] {
        return this.wrapConletNodes(document.querySelectorAll(
            ".conlet[data-conlet-id='" + conletId + "']"
            ) as NodeListOf<HTMLElement>);
    }

    /**
     * Find the conlet representations that display content. 
     * The default implementation wraps all nodes that match 
     * `body .conlet.conlet-content`. If a `conletId` is specified,
     * the the result set is restricted to conlets with this id.
     * 
     * @param conletId the conlet id
     * @return the conlets found
     */
    findConletContents(conletId?: string): Conlet[] {
        if (conletId) {
            return this.wrapConletNodes(<NodeListOf<HTMLElement>>
                document.querySelectorAll(
                ":scope body .conlet.conlet-content[data-conlet-id='" 
                + conletId + "']"));
        }
        return this.wrapConletNodes(document.querySelectorAll(
            ":scope body .conlet.conlet-content") as NodeListOf<HTMLElement>);
    }

    /**
     * Find conlet representations that display content and
     * are embedded in the given conlet (which is most likely a view
     * or preview, but may also be a content representation).
     * The default implementation wraps all nodes (with the exception
     * of the argument's element) that match 
     * `.conlet.conlet-content` and returns them in a depth first order.
     * 
     * @param conlet the containing conlet
     * @return the conlets found
     */
    findEmbeddedConlets(conlet: Conlet): Conlet[] {
        let result: Conlet[] = [];
        for (let i = 0; i < conlet.element().children.length; i++) {
            let child = conlet.element().children[i];
            if (!(child instanceof HTMLElement)) {
                continue;
            }
            this._findEmbeddedConlets(result, child);
        }
        return result;
    }

    private _findEmbeddedConlets(conlets: Conlet[], element: HTMLElement) {
        for (let i = 0; i < element.children.length; i++) {
            if (element.children[i] instanceof HTMLElement) {
                this._findEmbeddedConlets(conlets,
                    <HTMLElement>element.children[i]);
            }
        }
        if (element.classList.contains("conlet")
            && element.classList.contains("conlet-content")
            && element.dataset["conletId"]) {
                conlets.push(new DefaultConlet(element));
        }
    }

    /**
     * Find the conlet representation that displays the preview of the
     * conlet with the given id. The default implementation wraps
     * the node with `.conlet-preview[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the conlet or null
     */
    findConletPreview(conletId: string): Conlet | null {
        return this.wrapConletNode(document.querySelector(
            ".conlet-preview[data-conlet-id='" + conletId + "']"));
    }

    /**
     * Find the HTML element that displays a modal dialog associated
     * with the conlet with the given id. The default implementation returns
     * all nodes with `.conlet-modal-dialog[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the HTML element or null
     */
    findModalDialog(conletId: string): HTMLElement | null {
        return document.querySelector(
            ".conlet-modal-dialog[data-conlet-id='" + conletId + "']");
    }

    /**
     * Return all conlet preview representations. The default
     * implementation wraps all elements with 
     * `.conlet-preview[data-conlet-id]`.
     */
    findPreviews(): Conlet[] {
        return this.wrapConletNodes(
            document.querySelectorAll(".conlet-preview[data-conlet-id]"));
    }

    /**
     * Find the conlet representation that displays the view of the
     * conlet with the given id. The default implementation returns
     * all nodes with `.conlet-view[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the conlet
     */
    findConletView(conletId: string): Conlet | null {
        return this.wrapConletNode(document.querySelector(
            ".conlet-view[data-conlet-id='" + conletId + "']"));
    }

    /**
     * Return all conlet view representations. The default 
     * implementation wraps all nodes with `.conlet-view[data-conlet-id]`.
     *
     * @return
     */
    findViews() {
        return this.wrapConletNodes(
            document.querySelectorAll(".conlet-view[data-conlet-id]"));
    }
}

export default Renderer;
export { Conlet, DefaultConlet };
