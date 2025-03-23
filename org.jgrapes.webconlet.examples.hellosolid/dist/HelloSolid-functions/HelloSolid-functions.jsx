/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2025  Michael N. Lipp
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
'use strict';
import { createSignal } from "solid-js";
import { render } from "solid-js/web";
window.orgJGrapesConletsHelloSolidConlet = {};
window.orgJGrapesConletsHelloSolidConlet.initPreview
    = function (content, isUpdate) {
        const [name, setName] = createSignal("World");
        render(() => <div><h1>Hello {name()}</h1></div>, content);
    };
window.orgJGrapesConletsHelloSolidConlet.onUnload
    = function (content, isUpdate) {
        /*    if (!isUpdate) {
                JGConsole.notification("World Removed!", { type: "warning",
                    autoClose: 5000 });
            }
        */ 
    };
