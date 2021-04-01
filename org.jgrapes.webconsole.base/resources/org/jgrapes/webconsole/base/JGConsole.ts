/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2021  Michael N. Lipp
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
import { nodeFromString, formatMemorySize } from "./Util";
import Log from "./Log";
import Console from "./Console";
import Renderer from "./Renderer";
import RenderMode from "./RenderMode";
import NotificationOptions, { NotificationType } from "./NotificationOptions";
import TableController from "./TableController";
import OptionsSet from "./OptionsSet";

// Re-export
export { Console, nodeFromString, formatMemorySize, Renderer, RenderMode, 
    NotificationType, NotificationOptions, TableController, OptionsSet };

/** The singleton that provides the console instance. */
export const theConsole = new Console();

/** 
 * A class with static members that makes the console and the types 
 * used by the API available together with some utilty functions.
 * Usually imported as `JGConsole`. Of course, the types and functions
 * may also be imported individually.
 *
 * The class is also registered as `window.JGConsole` for non-modular 
 * JavaScript.   
 */
export default class JGConsole {
    static instance = theConsole;
    static Log = Log;
    static Renderer = Renderer;
    static RenderMode = RenderMode;
    static NotificationType = NotificationType;
    static TableController = TableController;
    static OptionsSet = OptionsSet;
    static nodeFromString = nodeFromString;
    static formatMemorySize = formatMemorySize;
    
    /**
     * Delegates to {@link Console#init}.
     */
    static init(consoleSessionId: string, refreshInterval: number,
        inactivityTimeout: number) {
        this.instance.init(consoleSessionId, refreshInterval,
            inactivityTimeout);
    }

    /**
     * Delegates to {@link Console#registerConletFunction}.
     */
    static registerConletFunction(conletClass: string, functionName: string,
        conletFunction: (conletId: string, ...args: any[]) => void) {
        this.instance.registerConletFunction(conletClass, functionName,
            conletFunction);
    }

    /**
     * Delegates to {@link Console#notifyConletModel}.
     */
    static notifyConletModel(conletId: string, method: string, ...params: any[]) {
        return theConsole.notifyConletModel(conletId, method, ...params);
    }

    /**
     * Delegates to {@link Console#lockMessageQueue}.
     */
    static lockMessageQueue() {
        this.instance.lockMessageQueue();
    }

    /**
     * Delegates to {@link Console#unlockMessageQueue}.
     */
    static unlockMessageQueue() {
        this.instance.unlockMessageQueue();
    }

    /**
     * Delegates to the instance's {@link Console#findConletPreview}.
     */
    static findConletPreview(conletId: string) {
        return this.instance.findConletPreview(conletId);
    }

    /**
     * Delegates to the instance's {@link Console#findConletView}.
     */
    static findConletView(conletId: string) {
        return this.instance.findConletView(conletId);
    }

    /**
     * Delegates to the instance's {@link Console#notification}.
     */
    static notification(content: string, options: NotificationOptions = {}) {
        return this.instance.notification(content, options);
    }

    /**
     * Generates a UUID. 
     * @return
     */
    static uuid() {
        var r = crypto.getRandomValues(new Uint8Array(16));
        r[6] = r[6] & 0x0f | 0x40;
        r[8] = r[8] & 0x3f | 0x80;
        return (r[0].toString(16) + r[1].toString(16) +
            r[2].toString(16) + r[3].toString(16) +
            "-" + r[4].toString(16) + r[5].toString(16) +
            "-" + r[6].toString(16) + r[7].toString(16) +
            "-" + r[8].toString(16) + r[9].toString(16) +
            "-" + r[0].toString(10) + r[0].toString(11) +
            r[0].toString(12) + r[0].toString(13) +
            r[0].toString(14) + r[0].toString(15));
    };

    /**
     * Finds the lang specific item in a map of items by language.
     * The function first tests for a property as specified by lang,
     * then removes any trailing "-..." from lang and tries again.
     * If not successful, it tests for an entry using "en" and 
     * if still no match is found it returns null. 
     * 
     * @param items the messages by language identifier
     * @param lang the language identifier
     * @param fallback fallback language (defaults to 'en')
     */
    static forLang(items: Map<string, Map<string,string>> | Map<string,string>,
            lang: string, fallback = 'en'): Map<string,string> | string | null {
        if (items.has(lang)) {
            return items.get(lang)!;
        }
        let dashPos = lang.lastIndexOf("-");
        if (dashPos > 0) {
            return JGConsole.forLang(items, lang.substring(0, dashPos), fallback);
        }
        if (fallback && lang != fallback) {
            return JGConsole.forLang(items, fallback, fallback);
        }
        return null;
    }

    /**
     * Localizes the given key, using the provided localizations and language.
     *
     * First, the implementation looks up a mapping using 
     * `forLang(l10ns, lang, fallback)`. Then it looks for an entry for `key`.
     * If none is found, it returns the value of key.
     * 
     * @param l10ns the mappings by language identifier
     * @param lang the language identifier
     * @param key the key to look up
     * @param fallback fallback language (defaults to 'en')
     */
    static localize(l10ns: Map<string,Map<string,string>>, lang: string, 
            key: string, fallback = 'en') {
        let langMsgs = <Map<string,string>>this.forLang(l10ns, lang, fallback) 
            || new Map<string,string>();
        let result = langMsgs.get(key) || key;
        return result;
    }

    /**
     * Returns the data (`data-*` Attribute) of the specified
     * element with the given key. If it does not exist, it
     * set to the value provided by the supplier function.
     * 
     * @param node - the DOM element
     * @param key - the key of the data
     * @param supplier - the supplier function
     */
    static createIfMissing(node: HTMLElement, key: string, supplier: () => string) {
        let data = node.dataset[key];
        if (data) {
            return data;
        }
        data = supplier();
        node.dataset[key] = data;
        return data;
    }

};

export { JGConsole };

// For global access
declare global {
  interface Window {
    JGConsole: JGConsole;
  }
}
window.JGConsole = JGConsole;
