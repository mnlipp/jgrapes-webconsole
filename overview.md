Web Console components for the JGrapes framework.

JGrapes-WebConsole
==================

Web Console components for the JGrapes framework. The components build on
the core framework and components.

<object type="image/svg+xml" data="package-hierarchy.svg">Package hierarchy</object>

`org.jgrapes.webconsole`
: Components for building web consoles. **Make sure to read 
    <a href="org/jgrapes/webconsole/base/package-summary.html#package.description">this package's description</a>
    first.**
    
@startuml package-hierarchy.svg
skinparam svgLinkTarget _parent

package org.jgrapes {
    package "JGrapes Core Components" {
    }

    package org.jgrapes.webconsole.base [[org/jgrapes/webconsole/base/package-summary.html#package.description]] {
    }

    package org.jgrapes.webconsole.vuejs [[org/jgrapes/webconsole/vuejs/package-summary.html#package.description]] {
    }

    package org.jgrapes.webconsole.bootstrap4 [[org/jgrapes/webconsole/bootstrap4/package-summary.html#package.description]] {
    }

    package org.jgrapes.webconsole.jqueryui [[org/jgrapes/webconsole/jqueryui/package-summary.html#package.description]] {
    }

    package "org.jgrapes.webconsole.jqueryui.themes.*" {
    }
}

"JGrapes Core Components" <.. org.jgrapes.webconsole.base
org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.jqueryui"
org.jgrapes.webconsole.jqueryui <.. "org.jgrapes.webconsole.jqueryui.themes.*"
org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.bootstrap4"
org.jgrapes.webconsole.base <.. "org.jgrapes.webconsole.vuejs"

@enduml