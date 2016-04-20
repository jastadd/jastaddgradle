JastAddGradle
=============

Gradle plugin for JastAdd development.

Can be used just to generate Java code using JastAdd, or for building an
modular project using JFlex and Beaver for scanner and parser generation.

##Non-modular build

Because simple JastAdd projects have no need for modules, and may use other
scanner and parser generators than JFlex and Beaver. For such projects it's
probably better to use a non-modular build configuration.  Here we show how to
configure a non-modular build using the JastAdd Gradle plugin.

Non-modular builds can be set up using the Gradle task
`org.jastadd.JastAddTask` provided by this Gradle plugin:

    task generateJava(type: org.jastadd.JastAddTask) {
        outputDir = file('src/gen')
        sources = fileTree('src/jastadd')
    }


To add this task type to your build, you will need to add this at the start of
the `build.gradle` file:

    buildscript {
        repositories.mavenCentral()
        dependencies {
            classpath 'org.jastadd:jastaddgradle:1.10.0'
        }
    }
    apply plugin: 'jastadd'


You may also want to make the `compileJava` plugin depend on the generated code:

    apply plugin: 'java'
    compileJava.dependsOn 'generateJava' // Generate code before compiling.
    sourceSets.main.java.srcDir 'src/gen' // Compiles the generated code.


By combining these three parts, you will have a minimal Gradle build that
generates Java code using JastAdd!

##Modular build

The JastAdd Gradle plugin allows building modular projects that use JFlex and
Beaver by adding some default build tasks. A modular build can be configured by
calling `jastadd.configureModuleBuild()`.  The default build tasks then
configured to do three things:

1. Run JFlex to generate a scanner.
2. Run Beaver to generate a parser.
3. Run JastAdd to generate an attributed abstract grammar.

Unlike JastAdd itself, the Gradle plugin does not yet support other scanner and
parser generator tools for modular builds.  JastAddGradle may get support for
other scanner- and parser generators in the future.

###Example build

Building a modular JastAddGradle project requires first adding the dependencies for
the plugin to the start of the Gradle build script, in the `build.gradle` file:

    buildscript {
        repositories.mavenCentral()
        dependencies {
            classpath group: 'org.jastadd:jastaddgradle:1.10.0'
        }
    }


The `mavenCentral()` call makes Gradle use the central Maven repository for
fetching build script dependencies online.

JastAddGradle uses a _module_ concept. Modules are declared in files named
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


##Example Projects

Some example projects using JastAddGradle:

* [JastAdd Example: GradleBuild](http://jastadd.org/web/examples.php?example=GradleBuild)
* [FinalChecker - ExtendJ extension demo](https://bitbucket.org/extendj/final-checker)
