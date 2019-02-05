# JastAddGradle

[![Build Status](https://travis-ci.org/jastadd/jastaddgradle.svg?branch=master)](https://travis-ci.org/jastadd/jastaddgradle)

Gradle plugin for [JastAdd][1] development.

Can be used just to generate Java code using JastAdd, or for building a
modular project using JFlex and Beaver for scanner and parser generation.

## Gradle Compatibility

This plugin is tested with the following Gradle versions:

* 2.8
* 3.5
* 4.4.1
* 4.5

The Plugin is NOT compatible with these Gradle versions:

* 2.7

## Add the plugin to your build

To add the plugin to your project, add this to your `build.gradle` file:

    plugins {
      id "org.jastadd" version "1.13.1"
    }


This configures the plugin to be downloaded from the [Gradle plugin portal][2].


## Modular vs Non-Modular builds

The JastAdd Gradle plugin can be used either as a simple code generator,
or as a modular JastAdd project build system.

The non-modular mode can be used if you just want to use JastAdd to generate
some code. This is useful for simple JastAdd projects.

In the modular build mode, the JastAdd Gradle plugin figures out which JastAdd
source files to include in the build. The modular build can also generate a
Beaver parser and JFlex scanner if needed.


## Non-modular build setup

The easiest way to set up a small Gradle build to generate some Java code with
JastAdd is to use a non-modular JastAdd build.

Non-modular builds can be set up using the Gradle task
`org.jastadd.JastAddTask` provided by the JastAdd Gradle plugin:

    plugins {
      id "java"
      id "org.jastadd" version "1.13.0"
    }
    task generateJava(type: org.jastadd.JastAddTask) {
      outputDir = file('src/gen')
      sources = fileTree('src/jastadd')
      options = [ "--package=my.ast" ]
    }


You should also add these two lines to make the default `compileJava` task
depend on the JastAdd-generated code:

    compileJava.dependsOn 'generateJava' // Generate code before compiling.
    sourceSets.main.java.srcDir 'src/gen' // Compiles the generated code.


## Modular build setup

The JastAdd Gradle plugin can run JastAdd automatically with the right source files
if you use the modular build mode. The JastAdd Gradle plugin also adds tasks to
generate a JFlex scanner and a Beaver parser.

A modular build can be configured by calling `jastadd.configureModuleBuild()`.
This configures a couple of default tasks:

* `generateAst`- runs JastAdd to generate the AST classes.
* `generateScanner` - runs JFlex to generate a scanner.
* `preprocessParser` - preprocesses parser source files with [JastAddParser][3].
* `generateParser` - runs Beaver to generate a parser.

The JastAdd Gradle plugin does not yet support other scanner and parser
generator tools for modular builds, but if you don't include any scanner/parser
sources then scanner/parser will not be generated.


### Rebuilding

The JastAdd Gradle plugin tries to rebuild whenever you make changes that could
affect generated code, but in some cases it does not work perfectly.  To ensure
a full rebuild you can run `./gradlew clean`, or pass the `--rerun-tasks`
option to Gradle when building your project.

If an AST class has been deleted, it is usually necessary to run `./gradlew
clean` to ensure that the generated class is removed.

## Changing JastAdd Version

The JastAdd Gradle plugin sets a default JastAdd version for code generation,
but you can change this by specifying any version you would like to use. Just
add a dependency for the `jastadd2` configuration in your build script:

    dependencies {
      jastadd2 "org.jastadd:jastadd:2.3.0"
    }


### Example build

Configuring a modular build requires that you provide module specifications to
the JastAdd Gradle plugin. The module specifications are used to determine
which files need to be passed to JastAdd for code generation.

Modules are typically declared in files named `jastadd_modules`. The location
of these module files is specified using the `modules` command in the `jastadd`
configuration. For example:

    jastadd {
      configureModuleBuild() // Add default build tasks for a modular build.
      modules "jastadd_modules", "jastadd/java4/jastadd_modules" // Load these modules.
      module "my module" // Select module to build.
    }


A module file can look like this:

    module("my module") {
      imports "java8 frontend" // This module depends on "java8 frontend" from ExtendJ.

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


The `imports` statement is used to add a dependency to some other module.
The included files from all transitive dependencies are included into the
current module files.

## Changing JastAdd Options

The JastAdd Gradle plugin by default adds these options to the JastAdd code generation command:

    --rewrite=cnta
    --safeLazy
    --visitCheck=false
    --cacheCycle=false


These default options can be changed by specifying the `jastaddOptions`
configuration setting inside the `jastadd` block in `build.gradle`. For example:

    jastadd {
      configureModuleBuild()

      modules {
        module ("example") {
          jastadd {
            include '*.ast'
          }
        }
      }

      module = "example"
      astPackage = "example"
      jastaddOptions = [ "--rewrite=none", "--visitCheck=true" ]
    }


## Example Projects

Some example projects using the JastAdd Gradle plugin:

* [ExtendJ Extension Base](https://bitbucket.org/extendj/extension-base)
* [JastAdd Example: GradleBuild](http://jastadd.org/web/examples.php?example=GradleBuild)


[1]:http://jastadd.org/
[2]:https://plugins.gradle.org/plugin/org.jastadd
[3]:https://bitbucket.org/jastadd/jastaddparser
