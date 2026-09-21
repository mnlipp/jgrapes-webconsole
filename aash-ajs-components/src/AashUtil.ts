import Alpine from 'alpinejs';

/**
 * Converter interface for attribute-to-property round-trip conversion.
 */
export interface Converter<T> {
  /** Converts attribute string value (or null) to property value */
  fromAttribute(value: string | null): T;
  /** Converts property value to attribute string (or null) */
  toAttribute(value: T): string | null;
}

/**
 * Converts from attribute string to function, using `new Function`.
 * Handles arrow functions, function expressions, and `function` keyword.
 * Returns null if value is null or conversion fails.
 */
const functionConverter: Converter<((...args: any[]) => any) | null> = {
  fromAttribute(value: string | null): ((...args: any[]) => any) | null {
    if (!value) {
      return null;
    }

    try {
      return new Function(`return (${value})`)();
    } catch (error) {
      console.error('Failed to convert attribute to function:', value, error);
      return null;
    }
  },

  toAttribute(value: ((...args: any[]) => any) | null): string | null {
    return value ? value.toString() : null;
  }
};

/**
 * Default converter that auto-detects type from the attribute value.
 *
 * Detection rules (checked in order):
 * - null → null
 * - empty string → empty string
 * - value starts with `[` → JSON array
 * - value starts with `{` → JSON object
 * - value is `true` or `false` → boolean
 * - value matches numeric pattern → number
 * - otherwise → string
 *
 * For presence-based booleans, use `booleanConverter`.
 * For function-valued attributes, use `functionConverter`.
 */
/** @internal Return type of the auto-detect converter */
type AutoValue = string | boolean | number | object | any[];

export const defaultConverter: Converter<AutoValue | null> = {
  fromAttribute(value: string | null): AutoValue | null {
    if (value === null) {
      return null;
    }
    const trimmed = value.trim();
    if (trimmed.startsWith('[')) {
      try { return JSON.parse(trimmed); } catch { return trimmed; }
    }
    if (trimmed.startsWith('{')) {
      try { return JSON.parse(trimmed); } catch { return trimmed; }
    }
    if (trimmed === 'true') { return true; }
    if (trimmed === 'false') { return false; }
    if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
      return parseFloat(trimmed);
    }
    return trimmed;
  },
  toAttribute(value: AutoValue | null): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    if (typeof value === 'boolean') {
      return value ? 'true' : 'false';
    }
    if (typeof value === 'number') {
      return String(value);
    }
    if (Array.isArray(value)) {
      return JSON.stringify(value);
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return value;
  }
};

/**
 * Converter for presence-based boolean attributes.
 * Attribute present (even with empty value) → true, absent → false.
 */
export const booleanConverter: Converter<boolean> = {
  fromAttribute(value: string | null): boolean {
    return value !== null;
  },
  toAttribute(value: boolean): string | null {
    return value ? '' : null;
  }
};

var _idCounter = 0;

function aashId() {
  return "aash-" + (_idCounter++).toString();
};

/* ------------------------------------------------------------------ */
/* Decorator infrastructure – WeakMap-based (no reflect-metadata)     */
/* ------------------------------------------------------------------ */

/** @internal Attribute descriptor stored per prototype */
interface AttrDescriptor {
  /** HTML attribute name (kebab-case) */
  attrName: string;
  /** Converter for round-trip attribute ↔ property */
  converter: Converter<any>;
  /** Name of the setter property */
  propName: string;
}

/** @internal Stores attribute descriptors per prototype object */
const _attrStore = new WeakMap<object, AttrDescriptor[]>();

/** @internal Stores attribute descriptors per class (constructor) */
const _classAttrs = new WeakMap<object, AttrDescriptor[]>();

/** @internal Stores renderer method key per prototype */
const _rendererStore = new WeakMap<object, string>();

/** @internal Retrieves renderer method key for a class */
function getRendererKey(cls: object): string | undefined {
  return _rendererStore.get(cls);
}

/** @internal Retrieves attribute descriptors for a prototype */
function getAttrs(proto: object): AttrDescriptor[] {
  return _attrStore.get(proto) ?? [];
}

/** @internal Retrieves attribute descriptors for a class */
function getClassAttrs(cls: object): AttrDescriptor[] {
  return _classAttrs.get(cls) ?? [];
}

/** @internal Stores attribute descriptors for a prototype */
function setAttrs(proto: object, entries: AttrDescriptor[]): void {
  _attrStore.set(proto, entries);
}

/**
 * Decorator factory: marks a setter as an attribute-binding.
 *
 * @param attrName HTML attribute name. If omitted, defaults to the
 *                 setter property name (e.g. `set title` → `'title'`).
 * @param converter Converter for attribute string ↔ property value.
 *                  Defaults to defaultConverter (auto-detects type).
 *
 * Example:
 * ```ts
 * @Attribute('dialog-title')
 * set title(value: string) { this.ajsData.title = value; }
 *
 * @Attribute('l10n', functionConverter)
 * set l10n(value: ((k: string) => string) | null) { ... }
 * ```
 */
export function Attribute(
  attrName?: string,
  converter: Converter<any> = defaultConverter
): MethodDecorator {
  return (
    target: object,
    propertyKey: string | symbol,
    descriptor: PropertyDescriptor
  ) => {
    const resolvedAttr = attrName ?? String(propertyKey);
    const entry: AttrDescriptor = {
      attrName: resolvedAttr,
      converter,
      propName: String(propertyKey)
    };
    const existing: AttrDescriptor[] = getAttrs(target);
    existing.push(entry);
    setAttrs(target, existing);
    return descriptor;
  };
}

/**
 * Method decorator: marks a method as the component's renderer.
 *
 * The `aash-component` Alpine directive looks up the decorated method
 * via the class's prototype and invokes it. This replaces the previous
 * convention of hard-coding the method name "render".
 *
 * Example:
 * ```ts
 * class MyElement extends HTMLElement {
 *   @Renderer()
 *   protected renderComponent() {
 *     this.innerHTML = `<span>...</span>`;
 *   }
 * }
 * ```
 */
export function Renderer(): MethodDecorator {
  return (
    target: object,
    propertyKey: string | symbol,
    descriptor: PropertyDescriptor
  ) => {
    _rendererStore.set(target, String(propertyKey));
    return descriptor;
  };
}

/**
 * Class decorator: turns an HTMLElement subclass into an AashComponent
 * with automatic attribute observation and initialization.
 *
 * Captures all `@Attribute` decorated setters from the class prototype,
 * generates `static get observedAttributes()` and
 * `attributeChangedCallback(name, oldVal, newVal)`.
 *
 * On `connectedCallback`, initializes attributes from the DOM before
 * the component's own `connectedCallback` body runs.
 *
 * Assumes the decorated class extends HTMLElement directly
 * (no intermediate @AashComponent decorated classes).
 *
 * Example:
 * ```ts
 * @AashComponent
 * class MyElement extends HTMLElement {
 *   @Attribute('my-label')
 *   set label(v: string) { this.ajsData.label = v; }
 * }
 * ```
 */
/* eslint-disable @typescript-eslint/no-explicit-any */
export function AashComponent(): ClassDecorator {
  return (cls: any) => {
    /* Collect descriptors from this class's prototype. */
    const descriptors: AttrDescriptor[] = getAttrs(cls.prototype);
    setAttrs(cls.prototype, descriptors);

    /* Store on class for runtime access via WeakMap. */
    _classAttrs.set(cls, descriptors);

    /* Generate static get observedAttributes() */
    Object.defineProperty(cls, 'observedAttributes', {
      get(this: typeof HTMLElement): string[] {
        const attrs: AttrDescriptor[] = getClassAttrs(this);
        return attrs.map((a) => a.attrName);
      },
      configurable: true
    });

    /* Capture user-defined callbacks before replacement. */
    const userAttrCb = cls.prototype.attributeChangedCallback;
    const userConnCb = cls.prototype.connectedCallback;

    /* Generate attributeChangedCallback */
    cls.prototype.attributeChangedCallback = function (
      this: HTMLElement,
      name: string,
      _oldVal: string | null,
      newVal: string | null
    ): void {
      if (userAttrCb) {
        userAttrCb.call(this, name, _oldVal, newVal);
      }
      const attrs: AttrDescriptor[] = getClassAttrs(this.constructor);
      const match = attrs.find((a: AttrDescriptor) => a.attrName === name);
      if (match) {
        const value = match.converter.fromAttribute(newVal);
        (this as any)[match.propName] = value;
      }
    };

    /* Generate connectedCallback */
    cls.prototype.connectedCallback = function (this: HTMLElement): void {
      const attrs: AttrDescriptor[] = getClassAttrs(this.constructor);
      for (const entry of attrs) {
        const raw = this.getAttribute(entry.attrName);
        const value = entry.converter.fromAttribute(raw);
        (this as any)[entry.propName] = value;
      }
      if (userConnCb) {
        userConnCb.call(this);
      }
      if (!this.hasAttribute("x-data")) {
        this.setAttribute("x-data", "");
      }
      if (!this.hasAttribute("x-aash-component")) {
        this.setAttribute("x-aash-component", "");
      }
    };

    return cls;
  };
}

Alpine.directive('aash-component',
  (el, { value, modifiers, expression }, { Alpine, effect, cleanup }) => {
  const key = getRendererKey(Object.getPrototypeOf(el));
  if (key) {
    const render = (el as any)[key];
    if (render && typeof render === "function") {
      render.call(el);
    }
  }
});

export { functionConverter, aashId };
