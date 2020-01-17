Web Console components for the JGrapes framework.

JGrapes-WebConsole
==================

Web Console components for the JGrapes framework. The components build on
the core framework and components.

<object type="image/svg+xml" data="package-hierarchy.svg">Package hierarchy</object>

`org.jgrapes.webconsole`
: Components for building web consoles. **Make sure to read 
    <a href="org/jgrapes/webcon/base/package-summary.html#package.description">this package's description</a>
    first.**
    
@startuml package-hierarchy.svg
skinparam svgLinkTarget _parent

package org.jgrapes {
    package "JGrapes Core Components" {
    }

    package org.jgrapes.webcon.base [[org/jgrapes/webcon/base/package-summary.html#package.description]] {
    }

    package org.jgrapes.webcon.vuejs [[org/jgrapes/webcon/vuejs/package-summary.html#package.description]] {
    }

    package org.jgrapes.webcon.bootstrap4 [[org/jgrapes/webcon/bootstrap4/package-summary.html#package.description]] {
    }

    package org.jgrapes.webcon.jqueryui [[org/jgrapes/webcon/jqueryui/package-summary.html#package.description]] {
    }

    package "org.jgrapes.webcon.jqueryui.themes.*" {
    }
}

"JGrapes Core Components" <.. org.jgrapes.webcon.base
org.jgrapes.webcon.base <.. "org.jgrapes.webcon.jqueryui"
org.jgrapes.webcon.jqueryui <.. "org.jgrapes.webcon.jqueryui.themes.*"
org.jgrapes.webcon.base <.. "org.jgrapes.webcon.bootstrap4"
org.jgrapes.webcon.base <.. "org.jgrapes.webcon.vuejs"

@enduml