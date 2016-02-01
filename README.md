JastAddGradle
=============

Gradle plugin for JastAdd development.

What it does
------------

JastAddGradle is intended for building JastAdd projects. It does three things:

1. Runs JFlex to generate a scanner
2. Runs Beaver to generate a parser
3. Runs JastAdd to generate an attributed abstract grammar

Most JastAdd projects use JFlex and Beaver, however JastAdd itself does not
mandate that these particular tools are used.  JastAddGradle may get support
for alternate scanner- and parser generators in the future.

Using JastAddGradle
-------------------

The first thing to do in order to use the plugin is to add a build script
dependency to JastAddGradle in your `build.gradle`:

    buildscript {
        repositories.mavenCentral()
        repositories.mavenLocal()
        dependencies {
            classpath group: 'org.jastadd', name: 'jastaddgradle', version: '1.9.11'
        }
    }

The `mavenCentral()` call makes Gradle use the central Maven repository for
fetching build script dependencies online. The call to `mavenLocal()` makes
Gradle use your local maven repository as well, making it possible to build and
test JastAddGradle without a network connection.

To build and install JastAddGradle in your local maven repository, run this command:

    gradle install

Module Definitions
------------------

Central to JastAddGradle is the concept of _modules_. Modules are declared in
files named `jastadd_modules`. The location of these files is specified using
the `modules` command in the `jastadd` configuration. For example:

    jastadd {
        modules "jastadd_modules", "jastadd/java4/jastadd_modules" // Load these modules.
        module "my module" // Select module to use.
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
* [FinalChecker - ExtendJ extension demo](https://bitbucket.org/extendj/final-checker)
