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
 * Helps to manage options. The main addition to simply using
 * a Set are the toggle functions and the support for temporarily
 * disabling an option.
 */
class OptionsSet {

    private _enabled = new Map<string,string>();
    private _disabled = new Map<string,string>();

    /**
     * Creates a new option set.
     */
    constructor() {
    }

    /**
     * Sets the option with the associated value.
     * 
     * @param name - the option's name
     * @param value - the option's value
     */
    set(name: string, value: string) {
        this._enabled.set(name, value);
        this._disabled.delete(name);
    }

    /**
     * Returns the value of the given name.
     * 
     * @param name - the option's name
     */
    get(name: string) {
        return this._enabled.get(name);
    }

    /**
     * Returns the names of all enabled options.
     */
    getEnabled() {
        return this._enabled.keys();
    }

    /**
     * Returns the names of all disabled options.
     */
    getDisabled() {
        return this._disabled.keys();
    }

    /**
     * Returns the names of all options.
     */
    getAll() {
        return (new Set([...this._enabled.keys(), ...this._disabled.keys()]))
            .values();
    }

    /**
     * Clears the option set.
     */
    clear() {
        this._enabled.clear();
        this._disabled.clear();
    }

    /**
     * Deletes the option with the given name.
     * 
     * @param name - the option's name
     */
    delete(name: string) {
        this._enabled.delete(name);
        this._disabled.delete(name);
    }

    /**
     * Disables the option with the given name. The option
     * and its value are kept and can be re-enabled.
     * 
     * @param name - the option's name
     */
    disable(name: string) {
        if (this._enabled.has(name)) {
            this._disabled.set(name, this._enabled.get(name)!);
            this._enabled.delete(name);
        }
    }

    /**
     * Re-enables the option with the given name.
     * 
     * @param name - the option's name
     */
    enable(name: string) {
        if (this._disabled.has(name)) {
            this._enabled.set(name, this._disabled.get(name)!);
            this._disabled.delete(name);
        }
    }

    /**
     * Toggles the option with the given name, i.e. enables
     * it if it is disabled and disables it if it is enabled.
     * 
     * @param name - the option's name
     */
    toggleEnabled(name: string) {
        if (this._enabled.has(name)) {
            this.disable(name);
        } else {
            this.enable(name);
        }
    }

    /**
     * Sets the option with the given name if it is not
     * set and deletes it if it is enabled.
     * 
     * @param name - the option's name
     * @param value - the option's value
     */
    toggleIsSet(name: string, value: string) {
        if (this._enabled.has(name)) {
            this._enabled.delete(name);
        } else {
            this._enabled.set(name, value);
        }
    }
}

export default OptionsSet;
