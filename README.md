JastAdd-Gradle
==============

Gradle plugin for JastAdd development.

To install in local maven repo run

    gradle install

To publish to public maven repo run

    gradle uploadArchives

Using JastAddGradle
-------------------

The first thing to do in order to use the plugin is to add it in the build
configuration of your `build.gradle`:

    buildscript {
        repositories.mavenLocal()
        repositories.maven {
            url 'http://jastadd.org/mvn/'
        }
        dependencies {
            classpath group: 'org.jastadd', name: 'jastaddgradle', version: '1.8'
        }
    }

The URL `http://jastadd.org/mvn/` is the public JastAdd maven repository.  The
`mavenLocal()` line adds the local maven repository, so that you can build and
test JastAddGradle without a network connection.


