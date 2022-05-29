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
 * Easy access to logging.
 */
class Log {

    private static logDateTimeFormat = new Intl.DateTimeFormat(undefined, 
        <Intl.DateTimeFormatOptions>{
        dateStyle: "short",
        timeStyle: "short",
    });

    private static logDateTimeMillis = new Intl.NumberFormat(undefined, {
        minimumIntegerDigits: 1,
        minimumFractionDigits: 3,
        maximumFractionDigits: 3,
    });

    /**
     * Format the given date as appropriate for a log time stamp.
     *
     * @param date the date to format
     */
    static format (date: Date) {
        return this.logDateTimeFormat.format(date) 
            + this.logDateTimeMillis.format(date.getMilliseconds()/1000).substring(1);
    }

    /**
     * Output a debug message.
     * @param message the message to print
     */
    static debug (message: any) {
        if (console && console.debug) {
            console.debug(this.format(new Date()) + ": " + message);
        }
    }
    
    /**
     * Output an info message.
     * @param message the message to print
     */
    static info(message: any) {
        if (console && console.info) {
            console.info(this.format(new Date()) + ": " + message)
        }
    }
    
    /**
     * Output a warn message.
     * @param message the message to print
     */
    static warn(message: any) {
        if (console && console.warn) {
            console.warn(this.format(new Date()) + ": " + message);
        }
    }
    
    /**
     * Output an error message.
     * @param message the message to print
     */
    static error(message: any) {
        if (console && console.error) {
            console.error(this.format(new Date()) + ": " + message);
        }
    }
}

export default Log;

