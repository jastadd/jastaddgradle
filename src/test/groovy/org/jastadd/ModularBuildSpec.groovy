/* Copyright (c) 2017-2018, Jesper Öqvist
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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ModularBuildSpec extends Specification {
  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

  def setup() {
    buildFile = testProjectDir.newFile('build.gradle')
  }

  def 'jastadd.configureModuleBuild() exists'() {
    given:
    buildFile << """
      plugins {
        id 'java'
        id 'jastadd'
      }

      jastadd {
        configureModuleBuild()
      }
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments('--info')
        .build()

    then:
    result.output.contains('Configuring JastAdd')
  }

  def 'simple module definition and code generation'() {
    given:
    File settings = testProjectDir.newFile('settings.gradle')
    settings << """
    rootProject.name = "funlang"
    """
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      modules project.file('jastadd_modules')
      module 'funlang'

      genDir = '.'
      astPackage = 'ast'
    }
    """
    File modules = testProjectDir.newFile('jastadd_modules')
    modules << """
    module("funlang") {
      moduleName "FunLang 1.0"
      moduleVariant "backend"

      jastadd {
        include "*.ast"
      }
    }
    """
    File astFile = testProjectDir.newFile('funlang.ast')
    astFile << """
    Program ::= Fun*;
    Fun ::= <ID> Param*;
    Param ::= <ID>;
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('--info', 'compileJava')
        .withPluginClasspath()
        .build()

    then:
    result.output.contains('Configuring JastAdd build for funlang.')
    result.task(':generateAst').outcome == TaskOutcome.SUCCESS
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS

    // Check that JastAdd generated AST source files:
    File genDir = new File(testProjectDir.getRoot(), 'ast')
    genDir.exists()
    new File(genDir, 'ASTNode.java').exists()
    new File(genDir, 'ASTState.java').exists()
    new File(genDir, 'Program.java').exists()
    new File(genDir, 'Fun.java').exists()
    new File(genDir, 'Param.java').exists()
  }

  def 'useBeaver works'() {
    given:
    File settings = testProjectDir.newFile('settings.gradle')
    settings << """
    rootProject.name = "funlang"
    """
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      modules project.file('jastadd_modules')
      module 'funlang'

      useBeaver = true // Normally false if there are no parser files.
      genDir = '.'
      astPackage = 'ast'
    }
    """
    File modules = testProjectDir.newFile('jastadd_modules')
    modules << """
    module("funlang") {
      moduleName "FunLang 1.0"
      moduleVariant "backend"

      jastadd {
        include "*.ast"
      }
    }
    """
    File astFile = testProjectDir.newFile('funlang.ast')
    astFile << """
    Program ::= Fun*;
    Fun ::= <ID> Param*;
    Param ::= <ID>;
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateAst')
        .withPluginClasspath()
        .build()

    then:
    result.task(':generateAst').outcome == TaskOutcome.SUCCESS

    // Check that ASTNode extends beaver.Symbol:
    File genDir = new File(testProjectDir.getRoot(), 'ast')
    genDir.exists()
    new File(genDir, 'ASTNode.java').readLines().any { it.contains('extends beaver.Symbol') }
  }

  def 'module definition can include another module definition'() {
    given:
    File settings = testProjectDir.newFile('settings.gradle')
    settings << """
    rootProject.name = "funlang"
    """
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      modules project.file('main_mods')
      module 'funlang'

      genDir = '.'
      astPackage = 'ast'
    }
    """
    File modules1 = testProjectDir.newFile('main_mods')
    modules1 << """
    include("included_mods")
    """
    File modules2 = testProjectDir.newFile('included_mods')
    modules2 << """
    module("funlang") {
      moduleName "FunLang 1.0"
      moduleVariant "backend"

      jastadd {
        include "*.ast"
      }
    }
    """
    File astFile = testProjectDir.newFile('funlang.ast')
    astFile << """
    Program ::= Fun*;
    Fun ::= <ID> Param*;
    Param ::= <ID>;
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateAst')
        .withPluginClasspath()
        .build()

    then:
    result.task(':generateAst').outcome == TaskOutcome.SUCCESS

    // Check that JastAdd generated AST source files:
    File ast = new File(testProjectDir.getRoot(), 'ast')
    ast.exists()
    new File(ast, 'ASTNode.java').exists()
    new File(ast, 'ASTState.java').exists()
    new File(ast, 'Program.java').exists()
    new File(ast, 'Fun.java').exists()
    new File(ast, 'Param.java').exists()
  }

  def 'parser generation skipped when there are no parser sources'() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      modules project.file('jastadd_modules')
      module 'funlang'

      genDir = '.'
      astPackage = 'ast'
    }
    """
    File modules = testProjectDir.newFile('jastadd_modules')
    modules << """
    module("funlang") {
      moduleName "FunLang 1.0"
      moduleVariant "backend"
    }
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateParser')
        .withPluginClasspath()
        .build()

    then:
    result.task(':generateParser').outcome == TaskOutcome.SKIPPED
    result.task(':preprocessParser').outcome == TaskOutcome.SKIPPED
  }

  def 'scanner generation skipped when there are no scanner sources'() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      modules project.file('jastadd_modules')
      module 'funlang'

      genDir = '.'
      astPackage = 'ast'
    }
    """
    File modules = testProjectDir.newFile('jastadd_modules')
    modules << """
    module("funlang") {
      moduleName "FunLang 1.0"
      moduleVariant "backend"
    }
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateScanner')
        .withPluginClasspath()
        .build()

    then:
    result.task(':generateScanner').outcome == TaskOutcome.SKIPPED
  }

  def 'inline module definition and code generation'() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'jastadd'
    }

    jastadd {
      configureModuleBuild()

      // Inline module definitions.
      modules {
        module("funlang") {
          moduleName "FunLang 1.0"
          moduleVariant "backend"

          jastadd {
            include "*.ast"
          }
        }
      }

      module = 'funlang'

      genDir = '.'
      astPackage = 'ast'
    }
    """
    File astFile = testProjectDir.newFile('funlang.ast')
    astFile << """
    Program ::= Fun*;
    Fun ::= <ID> Param*;
    Param ::= <ID>;
    """
    File ast = new File(testProjectDir.getRoot(), 'ast')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('compileJava')
        .withPluginClasspath()
        .build()

    then:
    result.task(':generateAst').outcome == TaskOutcome.SUCCESS
    result.task(':compileJava').outcome == TaskOutcome.SUCCESS
  }

}
