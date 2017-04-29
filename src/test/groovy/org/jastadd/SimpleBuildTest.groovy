package org.jastadd

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BuildLogicFunctionalTest extends Specification {
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
}
