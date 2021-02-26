# Overview

This library provides easy to use vue components for generating 
unstyled "ARIA augmented Semantic HTML" (AaSH).

## Motivation

I came up with the idea of "ARIA augmented Semantic HTML" at the 
end of 2019 while developing the 
[JGrapes Web Console](https://mnlipp.github.io/jgrapes/WebConsole.html),
a micro service driven micro frontend (with run-time integration via 
JavaScript). Contrary to the ususal approaches of micro frontends,
it favors a common, exchangeable styling for all components.

This prevents the usage of the wide spread "invasive" CSS frameworks that
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