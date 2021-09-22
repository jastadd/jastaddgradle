/* Copyright (c) 2014-2018, Jesper Öqvist
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.jastadd

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*
import org.gradle.api.logging.*

class JastAddPlugin implements Plugin<Project> {

  void apply(Project project) {
    def jastadd = project.extensions.create('jastadd', JastAddExtension, project)

    project.configurations.create('jastadd2')
    project.configurations['jastadd2'].defaultDependencies { deps ->
      deps.add(project.dependencies.create('org.jastadd:jastadd:2.3.2'))
    }

    project.repositories {
      mavenCentral()
    }
  }

}

class ScannerConfig {
  /** The name of the generated scanner (default="Scanner"). */
  String name = 'Scanner'

  /**
   * Directory to generate scanner in.
   * Defaults to build/generated-src/scanner.
   */
  String genDir

  ScannerConfig(Project project) {
    this.genDir = project.file("${project.buildDir}/generated-src/scanner")
  }
}

class ParserConfig {
  /** Generated parser name (default="Parser"). */
  String name = 'Parser'

  /**
   * Directory to generate parser in.
   * Defaults to build/generated-src/parser.
   */
  String genDir

  ParserConfig(Project project) {
    this.genDir = project.file("${project.buildDir}/generated-src/parser")
  }
}

class JastAddExtension {
  private static final Logger LOG = Logging.getLogger(JastAddPlugin.class)

  Project project
  final ModuleLoader loader

  /** All loaded modules. */
  List modules = []

  /** All module sources. */
  List moduleSources = []

  /** Rag root directory for documentation generation. */
  String ragroot

  JastAddExtension(Project project) {
    loader = new ModuleLoader(this)
    this.project = project
    this.ragroot = project.rootDir
    this.genDir = project.file("${project.buildDir}/generated-src/ast")
    this.scanner = new ScannerConfig(project)
    this.parser = new ParserConfig(project)
    this.buildInfoDir = "${project.buildDir}/generated-resources/jastadd"
  }

  void configureModuleBuild() {
    LOG.info("Configuring JastAdd build for ${project.name}.")

    project.gradle.taskGraph.whenReady {
      // Run some checks before the build starts.
      if (!module) {
        LOG.warn("No target JastAdd module is configured for ${project}. "
            + 'Add jastadd.module = "..." to fix this.')
      }
      module?.checkEmptyIncludes(project)
    }

    project.configurations.create('jastaddParser')
    project.configurations['jastaddParser'].defaultDependencies { deps ->
      deps.add(project.dependencies.create('org.jastadd:jastaddparser:1.0.3'))
      deps.add(project.dependencies.create('net.sf.beaver:beaver-rt:0.9.11'))
    }

    project.configurations.create('jflex')
    project.configurations['jflex'].defaultDependencies { deps ->
      deps.add(project.dependencies.create('de.jflex:jflex:1.6.1'))
    }

    project.configurations.create('beaver')
    project.configurations['beaver'].defaultDependencies { deps ->
      deps.add(project.dependencies.create('net.sf.beaver:beaver-ant:0.9.11'))
    }

    // TODO: Should we use project.sourceSets.main.java.srcDir instead?
    project.compileJava.source "${project.buildDir}/generated-src/ast"
    project.compileJava.source "${project.buildDir}/generated-src/parser"
    project.compileJava.source "${project.buildDir}/generated-src/scanner"

    project.task('generateAst', type: JavaExec) {
      description 'Generates Java sources from JastAdd code.'

      onlyIf { module }

      inputs.files { moduleSources + (module ? module.files(project, 'jastadd') : []) }
      outputs.dir {
        // This closure is needed to delay reading the genDir setting.
        project.file(genDir)
      }

      classpath = project.configurations.jastadd2
      mainClass = 'org.jastadd.JastAdd'

      doFirst {
        def outdir = project.file(genDir)
        if (outdir.isDirectory()) {
          // Clean output directory.
          project.fileTree(outdir).visit{ file -> file.getFile().delete() }
        } else {
          outdir.mkdirs()
        }
        def addBeaverOption
        if (useBeaver == 'maybe') {
          addBeaverOption = !module.files(project, 'parser').isEmpty()
        } else {
          addBeaverOption = useBeaver
        }
        def beaverOption = addBeaverOption ? [ '--beaver' ] : []
        args (
            [ "--package=${astPackage}",
            "--o=${outdir.path}" ]
            + beaverOption
            + jastaddOptions
            + extraJastAddOptions
            + module.files(project, 'jastadd'))
      }
    }

    project.task('generateScanner', type: JavaExec) {
      description 'Generates scanner with JFlex.'

      // Generate scanner only if there are some source files.
      onlyIf { module && !module.files(project, 'scanner').isEmpty() }

      inputs.files { moduleSources + (module ? module.files(project, 'scanner') : []) }
      outputs.dir {
        // This closure is needed to delay reading the genDir setting.
        project.file(scanner.genDir)
      }

      classpath = project.configurations.jflex
      mainClass = 'jflex.Main'

      doFirst {
        def inputFiles = project.files(module.files(project, 'scanner'))
        def outdir = project.file(scanner.genDir)
        if (outdir.isDirectory()) {
          // Clean output directory.
          project.fileTree(outdir).visit{ file -> file.getFile().delete() }
        } else {
          outdir.mkdirs()
        }
        ant.concat(destfile: "${temporaryDir}/${scanner.name}.flex",
          binary: true, force: false) {
          inputFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
        }
        args ([ '-d', outdir.path, "${temporaryDir}/${scanner.name}.flex" ])
      }
    }

    project.task('ragdoc', type: JavaExec) {
      description 'Generates RagDoc metadata for this JastAdd project.'
      dependsOn 'generateAst'

      // RD-Builder dependency should be added by user in the ragdoc
      // configuration (only needed if ragdoc task is run).

      inputs.files {
        project.files(module.files(project, 'java')) + project.sourceSets.main.allJava.files
      }

      mainClass = 'org.extendj.ragdoc.RagDocBuilder'

      doFirst {
        def destDir = new File(project.docsDir, 'ragdoc')
        if (!destDir.isDirectory()) {
          destDir.mkdirs()
        }
        classpath = project.configurations.ragdoc
        def sourceFiles = project.sourceSets.main.allJava.files
        args ([ '-cp', project.sourceSets.main.compileClasspath.asPath,
            '-d', "$destDir",
            '-ragroot', "$ragroot" ]
            + project.files(module.files(project, 'java'))
            + sourceFiles)
      }
    }

    project.task('preprocessParser', type: JavaExec) {
      description 'Generates Beaver parser with JastAddParser.'

      // Generate parser only if there are some source files.
      onlyIf { module && !module.files(project, 'parser').isEmpty() }

      inputs.files { moduleSources + (module ? module.files(project, 'parser') : []) }
      outputs.file {
        project.file("${temporaryDir}/${parser.name}.beaver")
      }

      classpath = project.configurations.jastaddParser
      mainClass = 'org.jastadd.jastaddparser.Main'

      doFirst {
        def inputFiles = project.files(module.files(project, 'parser'))
        ant.concat(destfile: "${temporaryDir}/${parser.name}.all",
          binary: true, force: false) {
          inputFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
        }
        def allFile = project.file("${temporaryDir}/${parser.name}.all")
        def beaverFile = project.file("${temporaryDir}/${parser.name}.beaver")
        args ([ allFile.path, beaverFile.path ])
      }
    }

    project.task('generateParser', type: JavaExec, dependsOn: [ 'preprocessParser' ]) {
      description 'Generates parser with Beaver.'

      // Generate parser only if there are some source files.
      onlyIf { module && !module.files(project, 'parser').isEmpty() }

      def genDir = "${project.buildDir}/generated-src/parser"
      inputs.files {
        project.file("${project.preprocessParser.temporaryDir}/${parser.name}.beaver")
      }
      outputs.dir {
        // This closure is needed to delay reading the genDir setting.
        project.file(parser.genDir)
      }

      classpath = project.configurations.beaver
      mainClass = 'beaver.comp.run.Make'

      doFirst {
        def outdir = project.file(parser.genDir)
        if (!outdir.isDirectory()) {
          outdir.mkdirs()
        }
        def inputFile = project.file("${project.preprocessParser.temporaryDir}/${parser.name}.beaver")
        args ([ '-d', outdir.path, '-t', '-c', '-w', inputFile.path ])
      }
    }

    project.task('buildInfo') {
      description 'Generates a property file with the module name.'
      outputs.file { "${buildInfoDir}/BuildInfo.properties" }

      doLast {
        def date = new Date()
        ant.mkdir dir: "${buildInfoDir}"
        ant.propertyfile(file: "${buildInfoDir}/BuildInfo.properties") {
          entry(key: 'moduleId', value: module.name)
          entry(key: 'moduleName', value: module.moduleName())
          entry(key: 'moduleVariant', value: module.moduleVariant())
          entry(key: 'timestamp', value: date.format("yyyy-MM-dd'T'HH:mm'Z'"))
          entry(key: 'build.date', value: date.format('yyyy-MM-dd'))
        }
      }
    }

    project.task('cleanGen', type: Delete) {
      description 'Removes generated files.'
      delete {
        [
          "${project.buildDir}/generated-resources/jastadd",
          "${project.buildDir}/generated-src/ast",
          "${project.buildDir}/generated-src/scanner",
          "${project.buildDir}/generated-src/parser",
        ]
      }
    }

    project.clean.dependsOn 'cleanGen'
    project.compileJava.dependsOn 'generateAst', 'generateScanner', 'generateParser'
    project.processResources.dependsOn 'buildInfo'
  }

  /** Load module specifications. */
  void modules(String... modules) {
    modules.each { loader.load(project.file(it)) }
  }

  void modules(File... modules) {
    modules.each { loader.load(it) }
  }

  // Defines modules with a closure.
  void modules(Closure closure) {
    loader.load(closure, project.projectDir)
  }

  /** Module instance. */
  private JastAddModule module

  /** Set the target module. */
  public void setModule(String name) {
    if (module != null) {
      throw new InvalidUserDataException("Target module already selected!")
    }
    module = getModule(name)
    // Add Java sources included in modules to source set.
    project.compileJava.source project.files(module.files(project, 'java')),
        project.sourceSets.main.java
  }

  /** Get the target module instance. */
  public JastAddModule getModule() {
    module
  }

  public void addModuleSource(source) {
    moduleSources.add source
  }

  /** Supported Java version. */
  String javaVersion

  String astPackage

  /** AST generation directory. Defaults to build/generated-src/ast. */
  String genDir

  /** Directory where BuildInfo.properties is generated (including build timestamp). */
  String buildInfoDir

  /** Set to {@code true} or {@code false} in JastAdd configuration. */
  def useBeaver = 'maybe'

  /**
   * Default set of JastAdd options
   * (excluding package name, beaver setting, and output directory).
   */
  List<String> jastaddOptions = [
      '--rewrite=cnta',
      '--safeLazy',
      '--visitCheck=false',
      '--cacheCycle=false' ];

  /** List of extra options to append to the default option list. */
  List<String> extraJastAddOptions = [];

  ScannerConfig scanner
  ParserConfig parser

  public void addModule(module) {
    modules.add module
  }

  def getModule(name) {
    for (module in modules) {
      if (module.name == name) {
        return module
      }
    }
    throw new InvalidUserDataException("Unknown JastAdd module \"${name}\".")
  }
}
