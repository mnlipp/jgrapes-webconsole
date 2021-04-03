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

/**
 * Converts a string with HTML source to a single or multiple DOM trees.
 *
 * @return the root nodes of the DOM trees
 */
export function parseHtml(html: string): HTMLElement[] {
    return <HTMLElement[]>Array.from(document.createRange()
        .createContextualFragment(html).childNodes);
}

/**
 * Behaves like `querySelector` but includes the root element as candidate.
 *
 * @param root the root element
 * @param selector the selector expression
 */
export function rootQuery(root: Element, selector: string): Node | null {
    return root.matches(selector) && root || root.querySelector(selector);
}

/**
 * Utility function to format a memory size to a maximum
 * of 4 digits for the integer part by appending the
 * appropriate unit.
 * 
 * @param size the size value to format
 * @param digits the number of digits of the factional part
 * @param lang the language (BCP 47 code, 
 * used to determine the delimiter)
 */
export function formatMemorySize(size: number, digits: number | string, 
    lang: string | number | undefined) {
    if (lang === undefined) {
        lang = digits;
        digits = -1;
    }
    let scale = 0;
    while (size > 10000 && scale < 5) {
        size = size / 1024;
        scale += 1;
    }
    let unit = "PiB";
    switch (scale) {
        case 0:
            unit = "B";
            break;
        case 1:
            unit = "kiB";
            break;
        case 2:
            unit = "MiB";
            break;
        case 3:
            unit = "GiB";
            break;
        case 4:
            unit = "TiB";
            break;
        default:
            break;
    }
    if (digits >= 0) {
        return new Intl.NumberFormat(<string>lang, {
            minimumFractionDigits: <number>digits,
            maximumFractionDigits: <number>digits
        }).format(size) + " " + unit;
    }
    return new Intl.NumberFormat(<string>lang).format(size) + " " + unit;
}

