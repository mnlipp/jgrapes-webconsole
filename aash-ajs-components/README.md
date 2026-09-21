# Overview

This library provides easy to use AlpineJS-based web components for
generating unstyled "ARIA augmented Semantic HTML" (AaSH).

## Motivation

I came up with the idea of "ARIA augmented Semantic HTML" at the 
end of 2019 while developing the 
[JGrapes Web Console](https://mnlipp.github.io/jgrapes/WebConsole.html),
a micro service driven micro frontend (with run-time integration via 
JavaScript). Contrary to the ususal approaches of micro frontends,
it favors a common, exchangeable styling for all components.

This prohibits the usage of the wide spread "invasive" CSS frameworks that
spread their presentation classes all over the HTML. The alternative is
to use the information provided by the (semantic) HTML for selecting
the styles to be applied. The problem is that even semantic HTML 5 doesn’t 
provide enough context to reliably style GUI widgets. If however, you add 
[WAI-ARIA](https://www.w3.org/WAI/standards-guidelines/aria/) attributes 
to the markup (as you should anyway), it turns out that almost all styling 
can be based on the HTML without adding presentation classes.
(A rough outline of the idea can also be found on 
"[css-tricks.com](https://css-tricks.com/aria-in-css/)".)

As it turns out, writing ARIA compliant HTML isn't as easy as you might think.
W3C maintains a document about 
[best practices](https://www.w3.org/TR/wai-aria-practices-1.1/) that also 
includes 
[some examples](https://www.w3.org/TR/wai-aria-practices-1.1/examples/).
Looking at the examples, it is obvious that ARIA compliant UI elements
must be generated from templates. This is where this library comes in.

## Status

This project is currently maintained as a subproject of the 
JGrapes Web Console project. It is continued as need (for the web console)
arises. In case there should be interest in using this independent of the
web console development, it will become a top level project of its own.

## Web Component Pattern

Each component follows a consistent pattern for creating AlpineJS-based
custom elements.

### Class Structure

```ts
import Alpine from 'alpinejs';
import {
  AashComponent, Attribute, Renderer, booleanConverter, functionConverter
} from '../../AashUtil';

@AashComponent()
export class MyComponent extends HTMLElement {

  private readonly ajsData: MyComponentData;

  constructor() {
    super();
    this.ajsData = Alpine.reactive(this.myComponentAlpineData());
  }

  protected myComponentAlpineData(): MyComponentData {
    return {
      someProperty: '',
      someMethod(): void { ... }
    } as MyComponentData;
  }

  protected connectedCallback() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    Alpine.initTree(this);
  }

  @Renderer()
  protected render() {
    this.innerHTML = `<span x-text="someProperty"></span>`;
  }

  @Attribute('some-prop')
  set someProperty(value: string) {
    this.ajsData.someProperty = value;
  }
}

customElements.define('my-component', MyComponent);
```

### Key Elements

1. **Reactive data** — `ajsData` holds an `Alpine.reactive()` object
   containing all state and methods that Alpine templates reference.

2. **Data factory** — `myComponentAlpineData()` returns the initial data
   object. Methods in this object have access to `this` (the reactive
   data) and can capture `element = this` (the component instance) for
   DOM operations.

3. **Lifecycle** — `connectedCallback` calls:
   - `Alpine.addScopeToNode()` — binds reactive data to the element
   - `Alpine.initTree()` — activates Alpine on the element
   - The `@Renderer()` decorated method is invoked by the
     `x-aash-component` Alpine directive after initialization

4. **Render** — The method decorated with `@Renderer()` sets `innerHTML`
   with Alpine directives:
   - `x-text` for text interpolation (no mustache syntax)
   - `x-bind` or `:` for attribute binding
   - `@event` for event handlers
   - `x-show`, `x-if` for conditional rendering
   - `x-for` for loops
   - `x-cloak` to hide content before Alpine initializes

5. **Property setters** — Public setters mutate `ajsData`, triggering
   Alpine's reactivity. Use `@Attribute` to bind setters to HTML
   attributes.

## Decorator Usage

### `@AashComponent()`

Class decorator that generates:

- `static get observedAttributes()` — lists all `@Attribute` decorated
  setters
- `attributeChangedCallback(name, oldVal, newVal)` — converts attribute
  string via the setter's converter and calls the setter
- Attribute initialization in `connectedCallback` — reads all registered
  attributes from the DOM and sets them before the component's own
  `connectedCallback` body runs

Usage:
```ts
@AashComponent()
class MyComponent extends HTMLElement { ... }
```

### `@Attribute(name?, converter?)`

Method decorator on a setter.

- `name` — HTML attribute name. Defaults to the setter property name.
- `converter` — Converter object with `fromAttribute` and `toAttribute`
  methods. Defaults to `defaultConverter` (auto-detects type).

Usage:
```ts
/* Default converter auto-detects string/number/boolean/object/array */
@Attribute('my-prop')
set myProp(value: string) { this.ajsData.myProp = value; }

/* Number auto-detected from attribute value "42" */
@Attribute()
set count(value: number) { this.ajsData.count = value; }

/* Boolean via "true"/"false" string or presence-based */
@Attribute('enabled')
set enabled(value: boolean) { this.ajsData.enabled = value; }

@Attribute('show-cancel', booleanConverter)
set showCancel(value: boolean) { this.ajsData.showCancel = value; }

/* Object/array auto-detected from JSON in attribute value */
@Attribute('config')
set config(value: object) { this.ajsData.config = value; }

/* Function converter for callback attributes */
@Attribute('on-action', functionConverter)
set onAction(value: ((...args: any[]) => any) | null) {
  this.ajsData.onAction = value;
}
```

### Built-in Converters

| Converter | Type | fromAttribute | toAttribute |
|-----------|------|---------------|-------------|
| `defaultConverter` | auto-detect | see below | type-dependent |
| `booleanConverter` | `boolean` | `v !== null` (presence) | `v ? '' : null` |
| `functionConverter` | `fn \|\| null` | `new Function(...)` | `v.toString()` |

`defaultConverter` auto-detects type from the attribute value:

| Value pattern | Result type |
|---------------|-------------|
| `null` / absent | `''` (empty string) |
| starts with `[` | JSON array |
| starts with `{` | JSON object |
| literal `true` / `false` | boolean |
| matches `-?\d+(\.\d+)?` | number |
| anything else | string |

For presence-based boolean attributes (e.g. `show-cancel`) use
`booleanConverter` instead.

### `@Renderer()`

Method decorator that marks a method as the component's renderer. The
`x-aash-component` Alpine directive looks up the decorated method via
the class's prototype and invokes it after Alpine initializes the tree.
This replaces the previous convention of hard-coding the method name
`"render"`.

Usage:
```ts
@Renderer()
protected render() {
  this.innerHTML = `<span x-text="someProperty"></span>`;
}
```

## Content Injection via x-teleport

Components that embed user-provided HTML fragments use a pattern based on
`<template>` elements and AlpineJS's `x-teleport` directive.

### Template

The consumer places `<template>` children inside the web component with a
`provides` attribute to declare what part of the widget the content fills:

```html
<aash-dropdown-menu id="lang-select">
  <template provides="label">
    <span>Language</span>
  </template>
  <template provides="item" with-value="en">
    <span>English</span>
  </template>
  <template provides="item" with-value="de">
    <span>German</span>
  </template>
</aash-dropdown-menu>
```

The `provides` attribute tells the component which role the template
serves. Additional attributes (e.g. `with-value`) are component-specific.

### Implementation

In `render` the component creates its structural HTML, assigning
a unique `id` (via `aashId()`) to every element that should receive user
content:

```ts
const labelTarget = aashId();
this.prepend(
  this.ownerDocument.createRange().createContextualFragment(
    `<button id="${labelTarget}" aria-haspopup="menu">
     </button>`));
```

`updateContent()` finds the `<template provides="...">` children and sets
an `x-teleport` attribute pointing to the corresponding target:

```ts
const label = this.querySelector(':scope > [provides="label"]');
if (label) {
  label.setAttribute("x-teleport", "#" + this.labelTarget);
}
```

For repeated slots (like menu items), the component generates a new target
for each template:

```ts
const items = this.querySelectorAll(':scope > [provides="item"]');
items.forEach(item => {
  const itemTarget = aashId();
  const li = document.createElement('li');
  li.innerHTML = `<button id="${itemTarget}"></button>`;
  item.setAttribute("x-teleport", "#" + itemTarget);
  ul.appendChild(li);
});
```

Alpine's `x-teleport` moves the template's inner content into the target
element at initialization time. A `MutationObserver` watching `childList`
on the host re-runs `updateContent()` when templates are added or removed,
keeping the rendered structure in sync.

To avoid the observer firing on the DOM mutations caused by `updateContent`
itself, the observer is disconnected before the update and re-connected
afterward.

### Rules

1. **Templates stay as direct children** — the component never removes or
   moves the original `<template>` elements.
2. **Teleport targets are unique** — each injection point gets its own
   `aashId()` so teleported content goes to the right place.
3. **Observer is childList-only** — watching `childList: true` on the host
   catches added/removed templates without triggering on inner changes.

## Component List

| Element | Description |
|---------|-------------|
| `aash-tablist` | Tab list with panel management |
| `aash-dropdown-menu` | Dropdown menu with click-outside handling |
| `aash-modal-dialog` | Modal dialog with ARIA attributes |
| `aash-disclosure-button` | Toggle button for element visibility |
| `aash-tree-view` | Tree view with keyboard navigation |
| `aash-accordion` | Collapsible accordion container |
| `aash-accordion-section` | Accordion section with animation |

## Building

```bash
npm run build
```

Output lands in `lib/` and is consumed by the provider module
`org.jgrapes.webconsole.provider.aashalpinejs`.
