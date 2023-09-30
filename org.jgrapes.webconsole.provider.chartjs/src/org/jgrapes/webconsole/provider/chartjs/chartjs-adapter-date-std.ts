/*
 * Copied from 
 * https://github.com/gcollin/chartjs-adapter-date-std/blob/master/src/index.ts
 * because the js distributed in npm cannot be loaded in browser.
 */

import {
  _adapters,
  ChartTypeRegistry,
  CoreChartOptions,
  DatasetChartOptions,
  DateAdapter,
  ElementChartOptions,
  PluginChartOptions,
  ScaleChartOptions,
  TimeUnit
} from "chart.js";
import { AnyObject } from "chart.js/types/basic"
import {_DeepPartialObject} from "chart.js/types/utils";

/*
 * An adaptor for the chartjs library enabling usage of time series without dependencies to any 3rd party libraries.
 * Based on the work of: https://github.com/mnlipp/jgrapes-webconsole/blob/master/org.jgrapes.webconsole.provider.chartjs/resources/org/jgrapes/webconsole/provider/chartjs/chart.js/adapters/chartjs-adapter-simple.js
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
export class StdDateAdapter implements DateAdapter<AnyObject> {

  /**
   * Creates an object from this StdAdaptorClass that can be used in Object.assign (...).
   * Needed for Chart.js
   */
  public static chartJsStandardAdapter ():any {
    console.log("Registering GraphJs Standard Date Adaptor");
    return {
      _id: "StdDataAdapter",

      delegate: new StdDateAdapter(),

      override: function (members: any) {
        return this.delegate.override(members);
      },

      init: function (chartOptions: any) {
        return this.delegate.init(chartOptions);
      },

      formats: function () {
        return this.delegate.formats();
      },

      parse: function (value: any, fmt: any) {
        return this.delegate.parse(value, fmt);
      },

      format: function (time: any, fmt: any) {
        return this.delegate.format(time, fmt);
      },

      add: function (time: any, amount: any, unit: any) {
        return this.delegate.add(time, amount, unit);
      },

      diff: function (max: any, min: any, unit: any) {
        return this.delegate.diff(max, min, unit);
      },

      startOf: function (time: any, unit: any, weekday: any) {
        return this.delegate.startOf(time, unit, weekday);
      },

      endOf: function (time: any, unit: any) {
        return this.delegate.endOf(time, unit);
      }
    }
  }

  formatLabels = ["datetime", "millisecond",
    "second", "minute", "hour", "day", "week", "month", "quarter", "year"];

  FORMATS: { [k: string]: string } = {};

  intlFormats: { [k: string]: Intl.DateTimeFormatOptions } = {
    'datetime': {
      year: "numeric", month: "short", day: "numeric",
      'hour': "numeric", minute: "numeric", second: "numeric"
    },
    'millisecond': {
      hour: "numeric", minute: "numeric", second: "numeric",
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
//@ts-ignore
      fractionalSecondDigits: 3
    },
    'second': {hour: "numeric", minute: "numeric", second: "numeric"},
    'minute': {hour: "numeric", minute: "numeric"},
    'hour': {hour: 'numeric'},
    'day': {day: 'numeric', month: 'short'},
    'week': {day: 'numeric', month: 'short', year:'numeric'},
    'month': {month: 'short', year: 'numeric'},
    'year': {year: 'numeric'}
  };

  formatters: { [k: string]: Intl.DateTimeFormat } = {};

  configuredLocale = "";
  options: AnyObject;

  updateLocale(locale: string): void {
    if (locale && locale != this.configuredLocale) {
      for (const label of this.formatLabels) {
        this.formatters[label] = new Intl.DateTimeFormat(locale, (this.intlFormats)[label]);
      }
      this.configuredLocale = locale;
    }
  }

  constructor(locale?:string) {
    this.updateLocale(locale || globalThis?.window?.navigator.language || "en-US");
    for (const label of this.formatLabels) {
      (this.FORMATS)[label] = label;
    }
    this.options = {};
  }

    override(members: Partial<DateAdapter>): void {
        //throw new Error("Method not implemented.");
    }

    init(chartOptions: _DeepPartialObject<CoreChartOptions<keyof ChartTypeRegistry> & ElementChartOptions<keyof ChartTypeRegistry> & PluginChartOptions<keyof ChartTypeRegistry> & DatasetChartOptions<keyof ChartTypeRegistry> & ScaleChartOptions<keyof ChartTypeRegistry>>): void {
        //throw new Error("Method not implemented.");
    }

  toInteger(dirtyNumber:any): number {
    if (dirtyNumber === null || dirtyNumber === true || dirtyNumber === false) {
      return NaN
    }

    const number = Number(dirtyNumber);

    if (isNaN(number)) {
      return number;
    }

    return number < 0 ? Math.ceil(number) : Math.floor(number);
  }

  toDate(value?: unknown): Date | null {
    if (value == null) {
      return null;
    }
    if (value instanceof Date) {
      return value;
    }
    if (typeof value === 'number') {
      return new Date(value);
    } else if (typeof value === 'string') {
      let number = Date.parse(value);
      if (isNaN(number)) {
        // Check if it's a trimester
        const val = (value as string).toUpperCase().trim ();
        let trimester=-1;
        // Brut force stuff
        let trimesterPos = val.indexOf("Q1");
        if (trimesterPos!=-1)
          trimester = 1;
        else {
          trimesterPos = val.indexOf("Q2");
          if (trimesterPos != -1)
            trimester = 2;
          else {
            trimesterPos = val.indexOf("Q3");
            if (trimesterPos != -1)
              trimester = 3;
            else {
              trimesterPos = val.indexOf("Q4");
              if (trimesterPos != -1)
                trimester = 4;
              else
                return null;
            }
          }
        }

        let year=NaN;
        const yearIndex = val.substring(trimesterPos).lastIndexOf(" ");
        if( yearIndex!=-1)
            year = Number.parseInt(val.substring(trimesterPos+yearIndex));
        if (isNaN(year))
          year = new Date().getFullYear();
//        console.log ("Trimester = "+trimester.toString()+" value = "+((trimester-1)*3+1));
        return new Date (year, ((trimester-1)*3+1));
      }else {
        return new Date (number);
      }
    }
    return null;

  }

  addMonths(date:Date, amount:number): number {
    const newDate = new Date(date.getTime());
    newDate.setMonth(date.getMonth() + amount);
    if (newDate.getMonth() != (date.getMonth() + amount) % 12) {
      newDate.setDate(0);
    }
    return newDate.getTime();
  }

  diffInMonths(max:Date, min:Date):number {
    let diff = (max.getFullYear() - min.getFullYear()) * 12;
    diff += max.getMonth() - min.getMonth();
    // Make sure that we have full months
    if (this.addMonths(min, diff) > max.getTime()) {
      diff -= 1;
    }
    return diff;
  }


  formats (): { [k:string]:string } {
    return this.FORMATS;
  }

  parse (value:unknown, fmt?:TimeUnit): number|null {
//    console.log("parsing ", value);
    const ret=this.toDate(value)?.getTime();
    if( ret==null)
      return null;
    else
      return ret;
  }

  format (time:number, fmt:TimeUnit): string {
    if( fmt === 'quarter') {
      // Special case
      const curDate = new Date (time);
  //    console.log ("Format quarter of ", curDate);
      return "Q"+Math.floor(curDate.getMonth()/3 + 1).toString()+" - "+curDate.getFullYear().toString();
    }
    if (!this.formatLabels.includes(fmt)) {
        return "Unknown";
    }
    return (this.formatters)[fmt].format(time);
  }

  add (time:number, amount:number, unit:TimeUnit):number {
    amount = this.toInteger(amount);
    if (isNaN(amount)) {
      return amount;
    }
    const date = this.toDate(time);
    if ((!amount) ||(date==null)) {
      return NaN;
    }
    switch (unit) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'week': amount *= 7;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'day': amount *= 24;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'hour': amount *= 60;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'minute': amount *= 60;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'second': amount *= 1000;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'millisecond': break;
      case 'year': return this.addMonths(date, amount * 12);
      case 'quarter': return this.addMonths(date, amount * 3);
      case 'month': return this.addMonths(date, amount);
      default: return time;
    }
    return time + amount;
  }

  diff (max:number, min:number, unit:TimeUnit):number {
    const maxDate=this.toDate(max);
    const minDate = this.toDate(min);
    if ((maxDate==null)||(minDate==null))
      return NaN;
    let diff = maxDate.getTime() - minDate.getTime();

    switch (unit) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'week': diff /= 7;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'day': diff /= 24;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'hour': diff /= 60;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'minute': diff /= 60;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'second': diff /= 1000;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      // eslint-disable-next-line no-fallthrough
      case 'millisecond': break;
      case 'month': return this.diffInMonths(maxDate, minDate);
      case 'quarter': return this.diffInMonths(maxDate, minDate) / 3;
      case 'year': return this.diffInMonths(maxDate, minDate) / 12;
      default: return 0;
    }

    if (diff < 0) {
      return Math.ceil(diff);
    }
    return Math.floor(diff);
  }

  startOf (time:number, unit:TimeUnit | 'isoWeek', weekday?:number): number {
    const date = this.toDate(time);
    if (date==null) return NaN;

    switch (unit) {
      case 'second': date.setMilliseconds(0); break;
      case 'minute': date.setSeconds(0, 0); break;
      case 'hour': date.setMinutes(0, 0, 0); break;
      case 'quarter': {
        const cur = date.getMonth()
        date.setMonth(cur - (cur % 3));
      };
      break;
      case 'month': date.setDate(1); break;
      case 'day': date.setHours(0, 0, 0, 0); break;
      case 'isoWeek':
      case 'week': {
        // Monday --> 0
        const cur = (date.getDay() + 6) % 7;
        date.setDate(date.getDate()-cur);
        date.setHours(0, 0, 0, 0); break;
      }
      case 'year': {
        const newDate = new Date(0);
        newDate.setFullYear(date.getFullYear(), 0, 1);
        newDate.setHours(0, 0, 0, 0);
        return newDate.getTime();
      }
      default: return date.getTime();
    }
    return date.getTime();
  }

  endOf (time:number, unit: TimeUnit | 'isoWeek'): number {
    const date = this.toDate(time);
    if( date == null)
      return NaN;
    switch (unit) {
      case 'second': date.setMilliseconds(999); break;
      case 'minute': date.setSeconds(59, 999); break;
      case 'hour': date.setMinutes(59, 59, 999); break;
      case 'day': date.setHours(23, 59, 59, 999); break;
      case 'isoWeek':
      case 'week': {
        // Monday --> 0
        const cur = (date.getDay() + 6) % 7;
        date.setDate(date.getDate()+6-cur);
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
        date.setMonth(curMonth - (curMonth % 3) + 3, 0);
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
    return date.getTime();
  }

}

// Automatically register the adapter
_adapters._date.override(StdDateAdapter.chartJsStandardAdapter());
