package org.jastadd

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class SimpleBuildTest extends Specification {
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
    File ast = new File(testProjectDir.getRoot(), 'ast')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateAst')
        .withPluginClasspath()
        .build()

    then:
    result.output.contains('Configuring JastAdd build for funlang.')

    // Check that JastAdd generated AST source files:
    ast.exists()
    assert new File(ast, 'ASTNode.java').exists()
    assert new File(ast, 'ASTState.java').exists()
    assert new File(ast, 'Program.java').exists()
    assert new File(ast, 'Fun.java').exists()
    assert new File(ast, 'Param.java').exists()
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
    File ast = new File(testProjectDir.getRoot(), 'ast')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('generateAst')
        .withPluginClasspath()
        .build()

    then:
    result.output.contains('Configuring JastAdd build for funlang.')

    // Check that JastAdd generated AST source files:
    ast.exists()
    assert new File(ast, 'ASTNode.java').exists()
    assert new File(ast, 'ASTState.java').exists()
    assert new File(ast, 'Program.java').exists()
    assert new File(ast, 'Fun.java').exists()
    assert new File(ast, 'Param.java').exists()
  }
}
