---
title: Using Icons with unstyled components
---

# Using Icons with unstyled components

A common approach to displaying icons is to assign a class to an element
that determines which icon to display. This makes it difficult to change
the icon later using CSS. 

## Replacing text with an icon

The approach proposed here is to treat an icon as an alternative visual
representation of text. We start with a textual description of an action 
such as `<button><span>Select language</span></button>`. For a more compact
presentation, the text can be visually replaced with an icon. This can
be achieved with CSS like this:

```scss
button {
    // Make text invisible, but preserve it for screen readers
    * {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip-path: inset(50%);
        white-space: nowrap;
        border: 0;
    }

    &::before {
        content: "";
        display: inline-block;
        width: 1em;
        height: 1em;
        vertical-align: middle;

        background-color: currentColor;
        mask-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0i...");
        mask-repeat: no-repeat;
        mask-position: center;
        mask-size: contain;
    }
}
```

Of course, using `button` as selector is too broad. It must be replaced
with a more specific selector that targets only the button used for
choosing the language.

Also note that any text must be wrapped in a child element to make
it invisible. There is no way to make DOM text (child) nodes invisible
without making the element itself invisible. Using `content: ""` comes
to mind, but using it on an element (not a pseudo-element) has no effect.

## Selecting elements

Unfortunately, neither HTML elements with their attributes nor ARIA
attributes (such as `role`) provide a reliable way to identify an element's
specific purpose. Users typically infer an element's purpose from its
text or, when no visible text is available, from an accessible name
provided through an attribute such as `aria-label`. However, both may be
localized, making them unsuitable for identifying elements in CSS selectors.

The approach proposed here is to use a `data-purpose` attribute to
specify an element's purpose. Possible values include:

<table>
  <thead>
    <tr>
      <th>Value</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>add-card</code></td>
      <td>Add a card to e.g. a dashboard.</td>
    </tr>
    <tr>
      <td><code>choose-language</code></td>
      <td>Choose a language.</td>
    </tr>
    <tr>
      <td><code>open-help</code></td>
      <td>Open a help dialog.</td>
    </tr>
    <tr>
      <td><code>open-settings</code></td>
      <td>Open a settings dialog.</td>
    </tr>
    <tr>
      <td><code>remove-item</code></td>
      <td>Remove an item. The common representation as an icon is "x".
        Removing should be interpreted in the widest sense. "Closing a
        dialog" is also considered "removing".</td>
    </tr>
    <tr>
      <td><code>show-expanded</code></td>
      <td>Display an expanded view.</td>
    </tr>
  </tbody>
</table>

These values are based on practical experience with the JGrapes Web Console
framework and are by no means exhaustive.

Since `data-purpose` can also be useful for styling the UI independently of
icon selection, it should not, by itself, imply that an element must be
represented by an icon. Instead, icon styling should be activated explicitly
through the `as-icon` class. The resulting CSS might look like this:

```scss
.as-icon {
    // Make text invisible, but preserve it for screen readers
    * {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip-path: inset(50%);
        white-space: nowrap;
        border: 0;
    }
    
    &::before {
        content: "";
        display: inline-block;
        width: 1em;
        height: 1em;
        vertical-align: middle;

        background-color: currentColor;

        mask-repeat: no-repeat;
        mask-position: center;
        mask-size: contain;
    }

    &[data-purpose="choose-language"]::before {
        mask-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0i...");
    }
}
```
