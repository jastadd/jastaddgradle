package org.jastadd

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class JastAddTaskTest extends Specification {
  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File genDir
  File buildFile
  File grammarFile

  def setup() {
    genDir = testProjectDir.newFolder('gen')
    buildFile = testProjectDir.newFile('build.gradle')
    grammarFile = testProjectDir.newFile('grammar.ast')
  }

  def 'generating single AST node with JastAddTask'() {
    def genPath = genDir.absolutePath.replace('\\', '\\\\')
    def grammarPath = grammarFile.absolutePath.replace('\\', '\\\\')

    given:
    grammarFile << """A;"""
    buildFile << """
      plugins {
        id 'java'
        id 'jastadd'
      }

      task generateJava(type: org.jastadd.JastAddTask) {
        outputDir = file('${genPath}')
        sources = files('${grammarPath}')
      }
    """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments('--info', 'generateJava')
        .build()

    then:
    !result.output.contains('Configuring JastAdd')
    new File(genDir, 'A.java').isFile()
  }
}
