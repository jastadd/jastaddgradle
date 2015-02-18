JastAddGradle
=============

Gradle plugin for JastAdd development.

What it does
------------

JastAddGradle (JAG) is intended for building JastAdd projects. It does three
things:

1. Runs JFlex to generate a scanner
2. Runs Beaver to generate a parser
3. Runs JastAdd to generate an attributed abstract grammar

Most JastAdd projects use JFlex and Beaver, however JastAdd itself does not
mandate that these particular third-party tools are used.  JAG may get support
for alternate scanner- and parser-generators in the future.

Using JastAddGradle
-------------------

The first thing to do in order to use the plugin is to add a dependency for in
the build configuration of your `build.gradle`:

    buildscript {
        repositories.mavenLocal()
        repositories.maven {
            url 'http://jastadd.org/mvn/'
        }
        dependencies {
            classpath group: 'org.jastadd', name: 'jastaddgradle', version: '1.9'
        }
    }

The URL `http://jastadd.org/mvn/` is the public JastAdd maven repository.  The
`mavenLocal()` line adds the local maven repository, so that you can build and
test JastAddGradle without a network connection.

To build and install JastAddGradle in your local maven repo:

    gradle install

Module Definitions
------------------

Central to JAG is the concept of _modules_. Modules are declared in
files named `modules`. The location of these files is specified using
the `modules` command in the `jastadd` configuration. For example:

    jastadd {
        modules ".", "jastadd/java4" // where to find module definitions
        module "my module" // select module to use
    }

A modules file can look like this:

    module("my module") {
        moduleName "My Module 1.0"
        moduleVariant "frontend"

        java {
            basedir "src/main/java/"
            include "**/*.java"
        }

        jastadd {
            include "grammar/*.ast"
            include "frontend/*.jadd"
            include "frontend/*.jrag"
        }

        scanner {
            include "scanner/Scanner.flex"
        }

        parser {
            include "parser/Parser.parser"
        }
    }

Example Projects
----------------

Some example projects using JastAddGradle:

* [JastAdd Example: GradleBuild](http://jastadd.org/web/examples.php?example=GradleBuild)
* [FinalChecker - JastAddJ extension demo](https://bitbucket.org/joqvist/finalchecker)

