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
import Console from "./Console";
import Log from "./Log";
import ConsoleNotification from "./Notification";
import NotificationOptions from "./NotificationOptions";
import ModalDialogOptions from "./ModalDialogOptions";
import RenderMode from "./RenderMode";
import { parseHtml } from "./Util";

/**
 * A base class for implementing a web console renderer. The renderer
 * creates the DOM for the SPA, usually based on some initial DOM from 
 * the console page. It also provides methods for the management of the 
 * conlets' representations in the DOM tree.
 */
export default abstract class Renderer {

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
     */
    addConletType(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[]) {
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
     */
    removeConletType(conletType: string) {
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
     * as component. The default implementation removes all children
     * from the container and inserts the new content.
     *
     * @param container the container, i.e. a `HTMLElement` returned
     * from {@link Renderer.findConletComponents}
     * @param content the component content as received from the server,
     * usually inserted into the container
     */
    updateConletComponent(container: HTMLElement, content: string) {
        let _this = this;
        let newContent = parseHtml(content)[0];
        while (container.firstChild) {
            container.removeChild(container.lastChild!);
        }
        container.append(newContent);
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
     * @param container the container for the preview. Either the result
     * from {@link Renderer.findConletPreview}, if an existing preview
     * is updated, or a prepared container for the preview,
     * provided as:
     * ```
     * <section class='conlet conlet-preview' 
     *      data-conlet-type='...' data-conlet-id='...' 
     *      data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>
     * ```
     * @param modes the supported conlet modes
     * @param content the preview content as received from the server,
     * usually inserted into the container
     * @param foreground `true` if the preview (i.e. the overview
     * plane) is to be made the active tab
     */
    updateConletPreview(isNew: boolean, container: HTMLElement, 
        modes: RenderMode[], content: string, foreground: boolean) {
        Log.warn("Not implemented!");
    }

    /**
     * Called from the {@link Console} to pdate the view of the given conlet.
     * If the view is new (no view with the
     * given conlet id exists), the console provides DOM for the
     * container as a convenience. Implementations of this method
     * are free to extract the data from the `conatiner` argument and 
     * provide their own markup for a new container
     *
     * @param isNew `true` if it is a new conlet view
     * @param container a prepared container for the view. Either the result
     * from {@link Renderer.findConletView}, if an existing preview
     * is updated, or a new container provided as:
     * ```
     * <article class="conlet conlet-view conlet-content 
     *      data-conlet-type='...' data-conlet-id='...'"></article>"
     * ```
     * @param modes the supported conlet modes
     * @param content the view content, usually inserted into the container
     * @param foreground `true` if the view 
     * is to be made the active tab
     */
    updateConletView(isNew: boolean, container: HTMLElement, 
        modes: RenderMode[], content: string, foreground: boolean) {
        Log.warn("Not implemented!");
    }

    /**
     * Called by the {@link Console} to remove the given conlet 
     * representations, which may be 
     * preview or view containers, from the DOM.
     *
     * @param containers the existing containers for
     * the preview or views 
     */
    removeConletDisplays(containers: HTMLElement[]) {
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
     * @param container the container for the dialog
     * @param options the options
     * @param content the content as HTML
     */
    openModalDialog(container: HTMLElement, options: ModalDialogOptions, 
        content: string) {
        Log.warn("Not implemented!");
    }

    /**
     * Called by the {@link Console} to close a modal dialog.
     * 
     * @param container the container for the dialog
     */
    closeModalDialog(container: HTMLElement) {
        Log.warn("Not implemented!");
    }

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
     * Find the HTML elements that display the preview or view of the
     * conlet with the given id. The default implementation returns
     * all nodes with `.conlet[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the elements found
     */
    findConlets(conletId: string): NodeListOf<HTMLElement> {
        return document.querySelectorAll(
            ".conlet[data-conlet-id='" + conletId + "']"
            ) as NodeListOf<HTMLElement>;
    };

    /**
     * Find the HTML elements that display a conlet component. 
     * The default implementation returns all nodes that match 
     * `body .conlet.conlet-component`.
     * 
     * @param conletId the conlet id
     * @return the elements found
     */
    findConletComponents(): NodeListOf<HTMLElement> {
        return document.querySelectorAll(
            ":scope body .conlet.conlet-component") as NodeListOf<HTMLElement>;
    }

    /**
     * Find the HTML element that displays the preview of the
     * conlet with the given id. The default implementation returns
     * all nodes with `.conlet-preview[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the HTML element or null
     */
    findConletPreview(conletId: string): HTMLElement | null {
        return document.querySelector(
            ".conlet-preview[data-conlet-id='" + conletId + "']");
    };

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
    };

    /**
     * Return the ids of all conlets displayed as preview. The default
     * implementation returns the values of `data-conlet-id` from
     * all elements with `.conlet-preview[data-conlet-id]`.
     */
    findPreviewIds(): string[] {
        return Array.from(
            document.querySelectorAll(".conlet-preview[data-conlet-id]"),
            node => node.getAttribute("data-conlet-id")!);
    }

    /**
     * Find the HTML element that displays the view of the
     * conlet with the given id. The default implementation returns
     * all nodes with `.conlet-view[data-conlet-id='<conletId>'`.
     * 
     * @param conletId the conlet id
     * @return the HTML element
     */
    findConletView(conletId: string): HTMLElement | null {
        return document.querySelector(
            ".conlet-view[data-conlet-id='" + conletId + "']");
    };

    /**
     * Return the ids of all conlets displayed as view. The default 
     * implementation returns the values of `data-conlet-id` from all
     * nodes with `.conlet-view[data-conlet-id]`.
     *
     * @return
     */
    findViewIds() {
        return Array.from(
            document.querySelectorAll(".conlet-view[data-conlet-id]"),
            node => node.getAttribute("data-conlet-id"));
    }
}

