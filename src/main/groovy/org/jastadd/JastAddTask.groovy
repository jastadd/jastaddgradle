package org.jastadd

import java.io.File

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.file.FileCollection

/**
 * A task that generates Java code from JastAdd specifications.
 * This task type is useful for simple and non-modular JastAdd builds.
 */
class JastAddTask extends JavaExec {
	@InputFiles
	FileCollection sources

	@OutputDirectory
	File outputDir

	@Input
	def options = []

	JastAddTask() {
		setMain('org.jastadd.JastAdd')
		setClasspath(project.configurations.jastadd2)
		outputDir = project.file('src/gen')
	}

	@Override
	@TaskAction
	public void exec() {
		// First, clean the destination directory so old generated files are removed.
		outputDir.eachFile { it.delete() }
		main = 'org.jastadd.JastAdd'
		args = [ "--o=$outputDir" ] + options + sources.files
		super.exec();
	}
}
