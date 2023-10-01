/*
 * Simple chart.js time axes adapter
 * Copyright (C) 2022  Michael N. Lipp
 * Released under the MIT license
 */

import { _adapters } from "../auto.js";

/*
 * Simple time axes adapter for chart.js.
 *
 *  * Handles Date and numeric data (as from Date.toTime()) only.
 *
 *  * Parsing simply calls the constructor of Date.
 *
 *  * Supported displayFormats are "datetime", "milliseconds", ...
 *    (i.e. key is passed through). Formatting is done with
 *    Intl.DateTimeFormat. Locale may be changed between calls to
 *    update.
 *
 *  * "weeks" and "quarters" is not supported by Intl.DateTimeFormat,
 *    the date is output instead.
 *
 *  * The helper functions (add, diff, etc.) have not really been tested.
 *
 *  * Superficial testing gives better performance results than moment 
 *    when using firefox as runtime.
 */

const formatLabels = ["datetime", "millisecond", 
    "second", "minute", "hour", "day", "week", "month", "quarter", "year"];

const FORMATS = {};

for (let label of formatLabels) {
    FORMATS[label] = label;
}

const intlFormats = {
  datetime: { year: "numeric", month: "short", day: "numeric",
    hour: "numeric", minute: "numeric", second: "numeric" },
  millisecond: { hour: "numeric", minute: "numeric", second: "numeric",
    fractionalSecondDigits: 3 },
  second: { hour: "numeric", minute: "numeric", second: "numeric" },
  minute: { hour: "numeric", minute: "numeric", second: "numeric" },
  hour: { hour: 'numeric' },
  day: { day: 'numeric', month: 'short'},
  week: { day: 'numeric', month: 'short'},
  month: {month: 'short', year: 'numeric'},
  quarter: {month: 'short', year: 'numeric'},
  year: { year: 'numeric' }
};

const formatters = {};

let configuredLocale = "";

function updateLocale(locale) {
    if (locale && locale != configuredLocale) {
        for (let label of formatLabels) {
            formatters[label] 
                = new Intl.DateTimeFormat(locale, intlFormats[label]);
        }
        configuredLocale = locale;
    }
}

updateLocale(navigator.language || "en-US");

/*
 * Next two have been taken from https://github.com/date-fns/date-fns.
 */

function toInteger(dirtyNumber) {
  if (dirtyNumber === null || dirtyNumber === true || dirtyNumber === false) {
    return NaN
  }

  const number = Number(dirtyNumber);

  if (isNaN(number)) {
    return number;
  }

  return number < 0 ? Math.ceil(number) : Math.floor(number);
}

function toDate(value) {
    if (value === null || typeof value === 'undefined') {
      return null;
    }
    if (value instanceof Date) {
        return value;
    }
    if (typeof value === 'number') {
        return new Date(value);
    }
    return null;
    
}

function addMonths(date, amount) {
    let newDate = new Date(date.getTime());
    newDate.setMonth(date.getMonth() + amount);
    if (newDate.getMonth() != (date.getMonth() + amount) % 12) {
        newDate.setDate(0);
    }
    return newDate;
}

function diffInMonths(max, min) {
    let diff = (max.getYear() - min.getYear()) * 12;
    diff += max.getMonth() - min.getMonth();
    // Make sure that we have full months
    if (addMonths(min, diff).getTime() > max.getTime()) {
        diff -= 1;
    }
    return diff;
}

let funcs = {
  _id: 'simple', // DEBUG

  formats: function() {
    return FORMATS;
  },

  parse: function(value, fmt) {
    return toDate(value).getTime();
  },

  format: function(time, fmt) {
    if (typeof time !== "number") {
        time = toDate(time).getTime();
    }
    if (!formatLabels.includes(fmt)) {
        return toDate(time).toDateString();
    }
    updateLocale(this.options.locale);
    return formatters[fmt].format(time);
  },

  add: function(time, amount, unit) {
    amount = toInteger(amount);
    if (isNaN(amount)) {
        return new Date(NaN)
    }    
    let date = toDate(time);
    if (!amount) {
        return date;
    }
    switch (unit) {
    case 'week': amount *= 7;
    case 'day': amount *= 24;
    case 'hour': amount *= 60;
    case 'minute': amount *= 60;
    case 'second': amount *= 1000;
    case 'millisecond': break;
    case 'year': return addMonths(date, amount * 12);
    case 'quarter': return addMonths(date, amount * 3);
    case 'month': return addMonths(date, amount);
    default: return time;
    }
    return new Date(date.getTime() + amount);
  },

  diff: function(max, min, unit) {
    let diff = toDate(max).getTime() - toDate(min).getTime();
    
    switch (unit) {
    case 'week': diff /= 7;
    case 'day': diff /= 24;
    case 'hour': diff /= 60;
    case 'minute': diff /= 60;
    case 'second': diff /= 1000;
    case 'millisecond': break;
    case 'month': return diffInMonths(max, min);
    case 'quarter': return diffInMonths(max, min) / 3;
    case 'year': return diffInMonths(max, min) / 12;
    default: return 0;
    }
    
    if (diff < 0) {
        return Math.ceil(diff);
    }
    return Math.floor(diff);
  },

  startOf: function(time, unit, weekday) {
    let date = toDate(time);
    switch (unit) {
    case 'second': date.setMilliseconds(0); break;
    case 'minute': date.setSeconds(0, 0); break;
    case 'hour': date.setMinutes(0, 0, 0); break;
    case 'quarter': {
        const cur = date.getMonth()
        date.setMonth(cur - (cur % 3));
    }  
    case 'month': date.setDate(1);
    case 'day': date.setHours(0, 0, 0, 0); break;
    case 'isoWeek':
    case 'week': {
        // Monday --> 0
        let cur = (date.getDay() + 6) % 7;
        date = funcs.diff(-cur, 'day');
        date.setHours(0, 0, 0, 0); break;
    }
    case 'year': {
        const newDate = new Date(0);
        newDate.setFullYear(date.getFullYear(), 0, 1);
        newDate.setHours(0, 0, 0, 0);
        return newDate;
    }
    default: return date;
    }
    return date;
  },

  endOf: function(time, unit) {
    const date = toDate(time);
    switch (unit) {
    case 'second': date.setMilliseconds(999); break;
    case 'minute': date.setSeconds(59, 999); break;
    case 'hour': date.setMinutes(59, 59, 999); break;    
    case 'day': date.setHours(23, 59, 59, 999); break;
    case 'isoWeek':
    case 'week': {
        // Monday --> 0
        let cur = (date.getDay() + 6) % 7;
        date = funcs.diff(6 - cur, 'day');
        date.setHours(23, 59, 59, 999); 
        break;
    }
    case 'month': {
        const month = date.getMonth();
        date.setFullYear(date.getFullYear(), month + 1, 0);
        date.setHours(23, 59, 59, 999);
        break;
    }
    case 'quarter': {
        const curMonth = date.getMonth();
        date.setMonth(currentMonth - (currentMonth % 3) + 3, 0);
        date.setHours(23, 59, 59, 999);
        break;
    }
    case 'year': {
        const year = date.getFullYear()
        date.setFullYear(year + 1, 0, 0)
        date.setHours(23, 59, 59, 999)
        break;        
    }
    default: return time;
    }
    return date;
  }
}

_adapters._date.override(funcs);
