# JastAddGradle

[![Build Status](https://travis-ci.org/jastadd/jastaddgradle.svg?branch=master)](https://travis-ci.org/jastadd/jastaddgradle)

Gradle plugin for JastAdd development.

Can be used just to generate Java code using JastAdd, or for building a
modular project using JFlex and Beaver for scanner and parser generation.

## Add the plugin to your build

The JastAdd Gradle plugin is only tested with Gradle 3.5, but it should work
for later Gradle versions.

To add the plugin to your project, add this to your `build.gradle` file:

    plugins {
      id "org.jastadd" version "1.12.0"
    }


This configures the plugin to be downloaded from the Gradle plugin portal.

## Modular vs Non-Modular builds

The JastAdd Gradle plugin can be used either as a simple code generator,
or as a modular JastAdd project build system.

The non-modular mode is useful if you want to build a very simple JastAdd project,
with a single module. In the non-modular mode, you create a new build task
to run JastAdd with your own provided source files.

In the modular mode, the JastAdd Gradle plugin figures out which JastAdd source
files to include in the build. The modular build can also generate a Beaver
parser and JFlex scanner if needed.


## Non-modular build setup

The easiest way to set up a small Gradle build to generate some Java code with
JastAdd is to use a non-modular JastAdd build.

Non-modular builds can be set up using the Gradle task
`org.jastadd.JastAddTask` provided by the JastAdd Gradle plugin:

    task generateJava(type: org.jastadd.JastAddTask) {
        outputDir = file('src/gen')
        sources = fileTree('src/jastadd')
    }


You may also want to make the `compileJava` plugin depend on the generated code:

    apply plugin: 'java'
    compileJava.dependsOn 'generateJava' // Generate code before compiling.
    sourceSets.main.java.srcDir 'src/gen' // Compiles the generated code.


## Modular build setup

The JastAdd Gradle plugin allows building modular projects that use JFlex and
Beaver by adding some default build tasks. A modular build can be configured by
calling `jastadd.configureModuleBuild()`.  A few default build tasks are
the generated:

1. Run JFlex to generate a scanner.
2. Run Beaver to generate a parser.
3. Run JastAdd to generate an attributed abstract grammar.

The JastAdd Gradle plugin does not yet support other scanner and parser
generator tools for modular builds.


### Rebuilding

The JastAdd Gradle plugin does not always detect if source files were modified.
To ensure a rebuild you can pass the `--rerun-tasks` option to Gradle when
building your project.

In particular, changes to JastAdd module specifications (`jastadd_modules` files)
always require a rebuild to take effect.

### Example build

The JastAdd Gradle plugin uses modules to figure out which files need to be passed
to JastAdd for code generation.  Modules are declared in files named
`jastadd_modules`. The location of these module files is specified using the
`modules` command in the `jastadd` configuration. For example:

    jastadd {
        configureModuleBuild() // Add default build tasks for a modular build.
        modules "jastadd_modules", "jastadd/java4/jastadd_modules" // Load these modules.
        module "my module" // Select module to build.
    }


A module file can look like this:

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


## Example Projects

Some example projects using JastAddGradle:

* [ExtendJ Extension Base](https://bitbucket.org/extendj/extension-base)
* [JastAdd Example: GradleBuild](http://jastadd.org/web/examples.php?example=GradleBuild)
