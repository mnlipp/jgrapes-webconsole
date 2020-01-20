JGrapes-WebConsole
==================

[![Build Status](https://travis-ci.org/mnlipp/jgrapes-webconsole.svg?branch=master)](https://travis-ci.org/mnlipp/jgrapes-webconsole)

| Package | Bintray |
| ------- | ------- |
| base    | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.base/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.base/_latestVersion)
| jqueryui | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.jqueryui/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.jqueryui/_latestVersion)
| bootstrap4 | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.bootstrap4/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.webconsole.bootstrap4/_latestVersion)

See the [project's home page](https://mnlipp.github.io/jgrapes/).

This repository comprises the sources for jars that provide
the web console components.

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
