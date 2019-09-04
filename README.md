JGrapes-Portal
==============

[![Build Status](https://travis-ci.org/mnlipp/jgrapes-portal.svg?branch=master)](https://travis-ci.org/mnlipp/jgrapes-portal)

| Package | Bintray |
| ------- | ------- |
| base    | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.base/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.base/_latestVersion)
| jqueryui | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.jqueryui/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.jqueryui/_latestVersion)
| bootstrap4 | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.bootstrap4/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.portal.bootstrap4/_latestVersion)

See the [project's home page](https://mnlipp.github.io/jgrapes/).

This repository comprises the sources for jars that provide
the portal components.

Themes
------

Additional themes are maintained in an 
[independant repository](https://github.com/mnlipp/jgrapes-portal-themes).

Demo
----

A [demo configuration](https://jgrapes-portal-demo.herokuapp.com/)
of the portal is available at [heroku](https://www.heroku.com/).

Building
--------

The libraries can be built with `gradle build`. For working with 
the project in Eclipse run `gradle eclipse` before importing the 
project. 

If you want to use 
[buildship](https://projects.eclipse.org/projects/tools.buildship),
import the project as "Gradle / Existing Gradle Project". Should you
encounter the (in)famous 
["sync problem"](https://github.com/eclipse/buildship/issues/478),
simply restart Eclipse.
