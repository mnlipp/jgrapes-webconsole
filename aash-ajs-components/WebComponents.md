---
title: Web component Pattern
---

# Web Component Pattern

Alpine JS does not have a standard pattern for implementing web
components. Therefore the library uses its own approach.
Each component follows this pattern for creating Alpine JS-based
custom elements.

## Class Structure

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
    this.ajsData = Alpine.reactive({
      someProperty: '',
      someMethod(): void { ... }
    } satisfies MyComponentData);
  }

  @Renderer()
  protected render() {
    Alpine.addScopeToNode(this,
      this.ajsData as unknown as Record<string, unknown>);
    this.innerHTML = `<span x-text="someProperty"></span>`;
  }

  @Attribute('some-prop')
  set someProperty(value: string) {
    this.ajsData.someProperty = value;
  }
}

customElements.define('my-component', MyComponent);
```

## Key Elements

1. **Reactive data** — `ajsData` holds an `Alpine.reactive()` object
   containing all state and methods that Alpine templates reference.

2. **Lifecycle** — `connectedCallback` (provided by the class decorator)
   sets the attributes `x-data` (if missing) and `x-aash-component`.
   The `x-aash-component` directive causes Alpine to call the method
   annotated with `@Renderer()`. If the component defines its own
   `connectedCallback`, the decorator invokes it before setting the
   attributes. This allows the component to initialize controller state
   before rendering (e.g. AashAccordionSection registers itself with
   the parent AashAccordion).

3. **Render** — The method decorated with `@Renderer()` makes the component's
   data available in the Alpine data stack via `Alpine.addScopeToNode()`
   and creates the `innerHTML` of the web component. Templates use
   Alpine directives (`x-text`, `x-bind`, `x-for`, `x-show`, `x-init`,
   `x-cloak`, etc.) for reactive rendering.

4. **Property setters** — Public setters mutate `ajsData`, triggering
   Alpine's reactivity. Use `@Attribute` to bind setters to HTML
   attributes. For programmatic API usage, expose plain setters without
   `@Attribute` (e.g. `onAction` on AashModalDialog).

5. **Cleanup** — Override `disconnectedCallback` to release resources
   (remove event listeners, disconnect `MutationObserver`s). The
   decorator chains the user's callback after its own setup, so
   cleanup code should not interfere with the decorator's lifecycle.

## Decorator Usage

### `@AashComponent()`

Class decorator that generates:

- `static get observedAttributes()` — lists all `@Attribute` decorated
  setters
- `attributeChangedCallback(name, oldVal, newVal)` — converts attribute
  string via the setter's converter and calls the setter
- Component preparation in `connectedCallback` — calls the component's
  own `connectedCallback` if it exists and then sets the attributes
  `x-data` (if missing) and `x-aash-component`.

Components that define their own `connectedCallback` (e.g. AashDisclosureButton,
AashAccordionSection) must manually call `Alpine.addScopeToNode()`, `render()`,
and `Alpine.initTree()` because the decorator's attribute-based initialization
via `x-aash-component` runs asynchronously after the element connects.

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
@Attribute('on-action', expressionConverter)
set onAction(value: ((...args: any[]) => any) | null) {
  this.ajsData.onAction = value;
}
```

### Built-in Converters

| Converter | Type | fromAttribute | toAttribute |
|-----------|------|---------------|-------------|
| `defaultConverter` | auto-detect | see below | type-dependent |
| `booleanConverter` | `boolean` | `v !== null` (presence) | `v ? '' : null` |
| `expressionConverter` | `fn \|\| null` | Alpine-evaluated | `v.toString()` |

`expressionConverter` converts the attribute string to a function. If called
with an `HTMLElement` argument (as done by the framework internally), it uses
`Alpine.evaluate(el, value)` to evaluate the expression within Alpine's
reactive context, allowing references to Alpine data properties. Without an
element argument it falls back to `new Function(...)`.

Typical usage for callback attributes:
```ts
@Attribute('on-action', expressionConverter)
set onAction(value: ((...args: any[]) => any) | null) {
  this.ajsData.onAction = value;
}
```

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

For repeated slots (like menu items), the preferred approach is to
include a `<template x-for=...>` in the component's template. The value
that `x-for` iterates over must be reactive data and is updated by
`updateContent()` based on the `<template provides="...">` children.
The `x-init` directive of elements generated in the loop is used to
set an `x-teleport` attribute pointing to the corresponding target.
The teleport must be scheduled via `Alpine.nextTick()` because setting
`x-teleport` during `x-init` evaluation is too early — Alpine has not
yet registered the target element. The `teleportItem` method in
AashDropdownMenu demonstrates this pattern.

Alpine's `x-teleport` moves the template's inner content into the target
element at initialization time (when the `render` method is called).
If the component supports dynamic changes of the injected content, 
it must define a `MutationObserver` watching `childList`. The observer
re-runs `updateContent()` when templates are added or removed,
keeping the rendered structure in sync.

To avoid the observer firing on the DOM mutations caused by `updateContent`
itself, the observer must be disconnected before the update and re-connected
afterward.

### Static Teleport (no loop)

For non-repeated slots (a single title, label, or content area), the
component sets `x-teleport` on the `<template>` directly in
`updateContent()`. The target element gets its content cleared first
so the teleported content replaces any default text. AashModalDialog
demonstrates this for its title, cancel-label, apply-label, okay-label,
and content slots.

### Rules

1. **Templates stay as direct children** — the component never removes or
   moves the original `<template>` elements.
2. **Teleport targets are unique** — each injection point gets its own
   `aashId()` so teleported content goes to the right place.
3. **Observer is childList-only** — watching `childList: true` on the host
   catches added/removed templates without triggering on inner changes.

## Building

```bash
npm run build
```

Output lands in `lib/` and is consumed by the provider module
`org.jgrapes.webconsole.provider.aashalpinejs`.
