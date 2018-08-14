Portal components for the JGrapes framework.

JGrapes-Portal
==============

Portal components for the JGrapes framework. The components build on
the core framework and components.

<object type="image/svg+xml" data="package-hierarchy.svg">Package hierarchy</object>

`org.jgrapes.portal`
: Components for building portals. **Make sure to read 
    <a href="org/jgrapes/portal/package-summary.html#package.description">this package's description</a>
    first.**
    
@startuml package-hierarchy.svg
skinparam svgLinkTarget _parent

package org.jgrapes {
    package "JGrapes Core Components" {
    }

    package org.jgrapes.portal.base [[org/jgrapes/portal/base/package-summary.html#package.description]] {
    }

    package org.jgrapes.portal.bootstrap4 [[org/jgrapes/portal/bootstrap4/package-summary.html#package.description]] {
    }

    package org.jgrapes.portal.jqueryui [[org/jgrapes/portal/jqueryui/package-summary.html#package.description]] {
    }

    package "org.jgrapes.portal.themes.*" {
    }
}

"JGrapes Core Components" <.. org.jgrapes.portal.base
org.jgrapes.portal.base <.. "org.jgrapes.portal.jqueryui"
org.jgrapes.portal.jqueryui <.. "org.jgrapes.portal.themes.*"
org.jgrapes.portal.base <.. "org.jgrapes.portal.bootstrap4"

@enduml