import Alpine from 'alpinejs';

/**
 * Converter interface for attribute-to-property round-trip conversion.
 */
export interface Converter<T> {
  /** Converts attribute string value (or null) to property value */
  fromAttribute(value: string | null, el?: HTMLElement): T;
  /** Converts property value to attribute string (or null) */
  toAttribute(value: T): string | null;
}

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
 * For expression-valued attributes, use `expressionConverter`.
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

/**
 * Converts from attribute string to function, using `new Function`.
 * Handles arrow functions, function expressions, and `function` keyword.
 * Returns null if value is null or conversion fails.
 */
export const functionConverter: Converter<((...args: any[]) => void) | null> = {
  fromAttribute(value: string | null): ((...args: any[]) => void) | null {
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
 * Converts from attribute string to function, using Alpine's
 * `evaluate`.
 */
export const expressionConverter: Converter<((...args: any[]) => void) | null> = {
  fromAttribute(value: string | null, el: HTMLElement): ((...args: any[]) => void) | null {
    if (!value) {
      return null;
    }
    return (...args: any[]) => {
      const wrapped = `(${value})(...${JSON.stringify(args)})`;
      Alpine.evaluate(el, wrapped);
    };
  },

  toAttribute(value: ((...args: any[]) => any) | null): string | null {
    return value ? value.toString() : null;
  }
};

var _idCounter = 0;

export function aashId() {
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

/** @internal All decorator metadata for a component prototype */
interface ComponentMetadata {
  attrs: AttrDescriptor[];
  rendererKey: string | undefined;
}

/** @internal Single WeakMap: prototype → metadata */
const _metadata = new WeakMap<object, ComponentMetadata>();

/** @internal Gets or creates metadata for a prototype */
function getMetadata(proto: object): ComponentMetadata {
  return _metadata.get(proto) ?? {
    attrs: [],
    rendererKey: undefined,
  };
}

/** @internal Ensures metadata is written back after mutation */
function ensureMetadata(proto: object, meta: ComponentMetadata): void {
  _metadata.set(proto, meta);
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
 * @Attribute('l10n', expressionConverter)
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
    const meta = getMetadata(target);
    meta.attrs.push(entry);
    ensureMetadata(target, meta);
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
 *     Alpine.addScopeToNode(this,
 *       this.ajsData as unknown as Record<string, unknown>);
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
    const meta = getMetadata(target);
    meta.rendererKey = String(propertyKey);
    ensureMetadata(target, meta);
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
 * On `connectedCallback`, sets the attributes `x-data` (if missing)
 * and `x-aash-comoponent` after the component's own `connectedCallback`
 * body runs (if provided).
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
    /* Capture user-defined callbacks before replacement. */
    const userAttrCb = cls.prototype.attributeChangedCallback;
    const userConnCb = cls.prototype.connectedCallback;

    /* Generate static get observedAttributes() */
    Object.defineProperty(cls, 'observedAttributes', {
      get(this: typeof HTMLElement): string[] {
        const meta = getMetadata(this.prototype);
        return meta.attrs.map((a) => a.attrName);
      },
      configurable: true
    });

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
      const meta = getMetadata(Object.getPrototypeOf(this));
      const match = meta.attrs.find((a: AttrDescriptor) => a.attrName === name);
      if (match) {
        const value = match.converter.fromAttribute(newVal, this);
        (this as any)[match.propName] = value;
      }
    };

    /* Generate connectedCallback */
    cls.prototype.connectedCallback = function (this: HTMLElement): void {
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
  (el, {}, { evaluate }) => {
  const meta = getMetadata(Object.getPrototypeOf(el));
  if (meta.rendererKey) {
    const render = (el as any)[meta.rendererKey];
    if (render && typeof render === "function") {
      render.call(el, evaluate);
    }
  }
});

