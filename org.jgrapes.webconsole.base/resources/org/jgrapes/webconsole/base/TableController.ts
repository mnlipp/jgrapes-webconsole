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

'use strict';

/**
 * A generic controller for tables. It provides information about
 * the available columns and maintains state regarding their
 * sort order and direction. In addition, it supports simple
 * filtering based on cell content.
 */
export default class TableController {

    private _keys: string[] = [];
    private _labelsByKey = new Map<string, string 
        | ((key: string) => string)>();
    private _sortKey = '';
    private _sortOrders = new Map<string, number>();
    private _filterKey: string | null = '';

    /**
     * Creates a new controller for a table with the given numer
     * of columns.
     * 
     * @param columns - the columns as a list
     *     of pairs of column key and column label. Labels
     *     may be functions which are invoked with the table controller
     *     as this and the key as argument if a label is required.
     * @param options.sortKey - the initial sort key
     * @param options.sortOrder - the initial sort order
     */
    constructor(columns: string[][], options: { sortKey: string, sortOrder: string }) {
        for (let i in columns) {
            this._keys.push(columns[i][0]);
            this._labelsByKey.set(columns[i][0], columns[i][1]);
        }
        for (let key of this._keys) {
            this._sortOrders.set(key, 1);
        }
        if (options) {
            if ("sortKey" in options) {
                this.sortBy(options.sortKey);
            }
            if ("sortOrder" in options) {
                this.sortBy(this._sortKey, options.sortOrder);
            }
        }
    }

    get keys() {
        return this._keys;
    }

    /**
     * Returns the column label for the given column key.
     * 
     * @param key - the column key
     */
    label(key: string) {
        let label = this._labelsByKey.get(key);
        if (typeof label === 'function') {
            return label.call(this, key);
        }
        return label;
    }

    /**
     * Returns the sort order of the column with the given key
     * (1 for "up" and -1 for "down").
     * 
     * @param key - the column key
     */
    sortOrder(key: string) {
        return this._sortOrders.get(key);
    }

    /**
     * This method sets the primary sort key. If the order is
     * `undefined`, and the current sort key is the same as the
     * specified key, the current sort order is inverted.
     * 
     * @param key - the column key
     * @param order - the sort order ('up' for ascending 
     *     and 'down' for descending) or `undefined`
     */
    sortBy(key: string, order?: string) {
        if (this._sortKey != key) {
            this._sortKey = key;
        }
        else {
            this._sortOrders.set(key, this._sortOrders.get(key)! * -1);
        }
        if (typeof order !== 'undefined') {
            if (order === 'up') {
                this._sortOrders.set(key, 1);
            }
            if (order === 'down') {
                this._sortOrders.set(key, -1);
            }
        }
    }

    /**
     * Returns `true` if given key is the current sort key 
     * and the current sort order for is ascending.
     * 
     * @param key - the column key
     */
    sortedByAsc(key:string) {
        return this._sortKey == key && this._sortOrders.get(key) == 1;
    }

    /**
     * Returns `true` if given key is the current sort key 
     * and the current sort order for is descending.
     * 
     * @param key - the column key
     */
    sortedByDesc(key: string) {
        return this._sortKey == key && this._sortOrders.get(key) == -1;
    }

    /**
     * Sort and filter the given data according to the current state
     * of the controller. Returns the sorted data.
     */
    filter(data: any[]) {
        let filterKey = this._filterKey && this._filterKey.toLowerCase();
        if (filterKey) {
            data = data.filter(function(item) {
                return Object.values(item).some(function(value) {
                    return String(value).toLowerCase().indexOf(filterKey!) > -1;
                });
            });
        }
        if (this._sortKey) {
            let sortKey = this._sortKey;
            let order = this._sortOrders.get(sortKey)!;
            data = data.sort(function(a, b) {
                a = a[sortKey];
                b = b[sortKey];
                return (a === b ? 0 : a > b ? 1 : -1) * order;
            });
        }
        return data;
    }

    /**
     * Sets a filter for the data.
     * 
     * @param filter - the string to match
     */
    filterBy(filter: string) {
        this._filterKey = filter;
    }

    /**
     * A convenience method to update the filter from the
     * value of the passed in event.
     * 
     * @param event - the event which must provide
     *     a value for `$(event.target).val()`. 
     */
    updateFilter(event: Event) {
        this._filterKey = (<any>(event.currentTarget!)).value;
    }

    /**
     * A convenience method for clearing an input element
     * that is used to specify a filter. Searches for 
     * an `input` element in the `event.target`'s enclosing
     * `form` element and sets its value to the empty string.
     * 
     * @param event - the event 
     */
    clearFilter(event: Event) {
        let form = (<Element>(event.currentTarget!)).closest("form");
        if (form) {
            let input = form.find("input");
            input.value = '';
            this._filterKey = '';
        }
    }

    /**
     * A convenience function that inserts word breaks
     * (`&#x200b`) before every dot in the given text
     * and returns the result.
     * 
     * @param text - the text
     */
    breakBeforeDots(text: string) {
        return String(text).replace(/\./g, "&#x200b;.");
    }
}

