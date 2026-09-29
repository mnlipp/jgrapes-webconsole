/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026  Michael N. Lipp
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

import JGConsole, { NotificationType } from "jgconsole";
import Alpine from "alpinejs";

interface HelloAlpineState {
    worldVisible: boolean;
}

/**
 * Store for conlet related state. An instance is made available
 * as global object.
 */
class HelloAlpineDataStore extends Map<string, HelloAlpineState> {

    // Provide functions as properties for calls from
    // data-jgwc-on-load and -unload
    onLoad!: (conletEl: HTMLElement, isUpdate: boolean) => void;
    onUnload!: (conletEl: HTMLElement, isUpdate: boolean) => void;

    addConletState(conletEl: HTMLElement) {
        const conletId = conletEl.closest("[data-conlet-id]")!
            .getAttribute("data-conlet-id")!;
        this.set(conletId, Alpine.reactive({ worldVisible: true }));
    }

    removeConletState(conletEl: HTMLElement, isUpdate: boolean) {
        const conletId = conletEl.closest("[data-conlet-id]")!
            .getAttribute("data-conlet-id")!;
        this.delete(conletId);
        if (!isUpdate) {
            JGConsole.notification("World Removed!",
                { type: NotificationType.Warning, autoClose: 5000 });
        }
    }
}

declare global {
    interface Window {
        orgJGrapesConletsExampleHelloAlpine: HelloAlpineDataStore;
    }
}

const conletStates = new HelloAlpineDataStore();
window.orgJGrapesConletsExampleHelloAlpine = conletStates;

conletStates.onLoad = function(conletEl: HTMLElement, _isUpdate: boolean) {
        conletStates.addConletState(conletEl);
    };

conletStates.onUnload = function(conletEl: HTMLElement, isUpdate: boolean) {
        conletStates.removeConletState(conletEl, isUpdate);
    };

JGConsole.registerConletFunction(
    "org.jgrapes.webconlet.examples.helloalpine.HelloAlpineConlet",
    "setWorldVisible",
    function(conletId: string, visible: boolean) {
        const state = conletStates.get(conletId);
        if (state) {
            state.worldVisible = visible;
        }
    });
