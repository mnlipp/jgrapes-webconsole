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

import JGConsole, { Console, NotificationOptions, NotificationType } from "jgconsole";
import { createFragment, aashId } from "aash-alpinejs";
import AlpineJsRenderer from "./AlpineJsRenderer";
import Alpine from "alpinejs";

var log = JGConsole.Log;

class NotificationEntry {
    id: string;
    content: string;
    options: NotificationOptions;

    constructor(content: string, options: NotificationOptions) {
        this.content = content;
        this.options = options;
        this.id = aashId();
    }

    notificationClass(): string {
        if (this.options.type === NotificationType.Error) {
          return "notification--error";
        }
        if (this.options.type === NotificationType.Success) {
            return "notification--success";
        }
        if (this.options.type === NotificationType.Warning) {
            return "notification--warning";
        }
        if (this.options.type === NotificationType.Danger) {
            return "notification--danger";
        }
        return "notification--info";
    }
}

export class NotificationManager {
    private renderer: AlpineJsRenderer;
    private console: Console;
    private notifications: NotificationEntry[] = Alpine.reactive([]);

    constructor(renderer: AlpineJsRenderer, console: Console) {
        this.console = console;
        this.renderer = renderer;
    }

    get entries() {
        return this.notifications;
    }
    
    notification(content: string, options: NotificationOptions): () => void {
        const entry = new NotificationEntry(content, options);
        this.notifications.push(entry);
        if (options.autoClose) {
            setTimeout(() => this.remove(entry.id), options.autoClose);
        }
        return () => {
        }
    }

    remove(id: string) {
        const idx = this.notifications.findIndex(e => e.id === id);
        if (idx >= 0) {
            this.notifications.splice(idx, 1);
        }
    }
}
