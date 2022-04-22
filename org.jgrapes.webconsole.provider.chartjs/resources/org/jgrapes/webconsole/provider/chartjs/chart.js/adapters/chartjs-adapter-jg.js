import { _adapters } from '../dist/chart.esm.js';
import { DateTime } from '../../luxon/build/es6/luxon.js';

/*!
 * chartjs-adapter-luxon v1.1.0
 * https://www.chartjs.org
 * (c) 2021 chartjs-adapter-luxon Contributors
 * Released under the MIT license
 * 
 * Modified for JGrapes WebConsole
 * Copyright (C) 2022  Michael N. Lipp
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

_adapters._date.override({
  _id: 'jg', // DEBUG

  /**
   * @private
   */
  _create: function(time) {
    return DateTime.fromMillis(time, this.options);
  },

  formats: function() {
    return FORMATS;
  },

  format: function(time, format) {
    if (typeof time === "Date") {
        time = time.getTime();
    }
    if (typeof time === "number" && formatLabels.includes(format)) {
        // Optimized usage of Intl.DateTimeFormat
        updateLocale(this.options.locale);
        return formatters[format].format(time);
    }
    
    // Use luxon
    const datetime = this._create(time);
    return typeof format === 'string'
      ? datetime.toFormat(format, this.options)
      : datetime.toLocaleString(format);
  },

  parse: function(value, format) {
    const options = this.options;

    if (value === null || typeof value === 'undefined') {
      return null;
    }

    const type = typeof value;
    if (type === 'number') {
      value = this._create(value);
    } else if (type === 'string') {
      if (typeof format === 'string') {
        value = DateTime.fromFormat(value, format, options);
      } else {
        value = DateTime.fromISO(value, options);
      }
    } else if (value instanceof Date) {
      value = DateTime.fromJSDate(value, options);
    } else if (type === 'object' && !(value instanceof DateTime)) {
      value = DateTime.fromObject(value);
    }

    return value.isValid ? value.valueOf() : null;
  },

  add: function(time, amount, unit) {
    const args = {};
    args[unit] = amount;
    return this._create(time).plus(args).valueOf();
  },

  diff: function(max, min, unit) {
    return this._create(max).diff(this._create(min)).as(unit).valueOf();
  },

  startOf: function(time, unit, weekday) {
    if (unit === 'isoWeek') {
      weekday = Math.trunc(Math.min(Math.max(0, weekday), 6));
      const dateTime = this._create(time);
      return dateTime.minus({days: (dateTime.weekday - weekday + 7) % 7}).startOf('day').valueOf();
    }
    return unit ? this._create(time).startOf(unit).valueOf() : time;
  },

  endOf: function(time, unit) {
    return this._create(time).endOf(unit).valueOf();
  }
});
