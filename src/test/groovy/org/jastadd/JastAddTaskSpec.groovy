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
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class JastAddTaskSpec extends Specification {
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
