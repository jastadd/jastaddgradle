import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*

class JastAddPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.task("jastaddTest") {
			doLast {
				def specFiles = project.files(
					JastAddModule.get(project.jastadd.module).files(project, "jastadd")
				)
				specFiles.each{ println "${it}" }
			}
		}
		project.task("generateJava", dependsOn: [ "scanner", "parser" ]) {
			inputs.files { JastAddModule.get(project.jastadd.module).files(project, "jastadd") }
			outputs.dir { project.file(project.jastadd.genDir) }
			doLast {
				def outdir = project.jastadd.genDir
				def specFiles = project.files(
					JastAddModule.get(project.jastadd.module).files(project, "jastadd")
				)
				ant.mkdir(dir: project.file(outdir))
				ant.taskdef(name: "jastadd", classname: "org.jastadd.JastAddTask",
					classpath: "${project.jastadd.toolsDir}/jastadd2.jar") { }
				ant.jastadd(
					package: project.jastadd.astPackage,
					rewrite: true,
					beaver: true,
					noVisitCheck: true,
					noCacheCycle: true,
					outdir: project.file(outdir),
					defaultMap: "new org.jastadd.util.RobustMap(new java.util.HashMap())") {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
			}
		}
		project.task("scanner") {
			inputs.files { JastAddModule.get(project.jastadd.module).files(project, "scanner") }
			outputs.dir { project.file("${project.jastadd.genDir}/scanner") }
			doLast {
				ant.mkdir(dir: "${project.jastadd.tmpDir}/scanner")
				def specFiles = project.files(
					JastAddModule.get(project.jastadd.module).files(project, "scanner")
				)
				ant.concat(destfile: "${project.jastadd.tmpDir}/scanner/JavaScanner.flex",
					binary: true, force: false) {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				ant.mkdir(dir: "${project.jastadd.genDir}/scanner")
				ant.taskdef(name: "jflex", classname: "JFlex.anttask.JFlexTask",
					classpath: "${project.jastadd.toolsDir}/JFlex.jar")
				ant.jflex(file: "${project.jastadd.tmpDir}/scanner/JavaScanner.flex",
					outdir: project.file("${project.jastadd.genDir}/scanner"),
					nobak: true)
			}
		}

		project.task("parser") {
			inputs.files { JastAddModule.get(project.jastadd.module).files(project, "parser") }
			outputs.dir { project.file("${project.jastadd.genDir}/parser") }
			doLast {
				ant.mkdir(dir: project.file(project.jastadd.tmpDir))
				def specFiles = project.files(
					JastAddModule.get(project.jastadd.module).files(project, "parser")
				)
				ant.concat(destfile: "${project.jastadd.tmpDir}/parser/JavaParser.all",
					binary: true, force: false) {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				ant.java(classname: "Main", fork: true) {
					classpath {
						pathelement(path: "${project.jastadd.toolsDir}/JastAddParser.jar")
						pathelement(path: "${project.jastadd.toolsDir}/beaver-rt.jar")
					}
					arg(value: "${project.jastadd.tmpDir}/parser/JavaParser.all")
					arg(value: "${project.jastadd.tmpDir}/parser/JavaParser.beaver")
				}
				ant.mkdir(dir: "${project.jastadd.genDir}/parser")
				ant.taskdef(name: "beaver", classname: "beaver.comp.run.AntTask",
					classpath: "${project.jastadd.toolsDir}/beaver-ant.jar")
				ant.beaver(file: "${project.jastadd.tmpDir}/parser/JavaParser.beaver",
					destdir: "${project.jastadd.genDir}/parser",
					terminalNames: true,
					compress: false,
					useSwitch: true)
			}
		}

		project.task("setSupportLevel") {
			outputs.dir { project.file(project.jastadd.genResDir) }
			doLast {
				ant.mkdir dir: "${project.jastadd.genResDir}"
				ant.propertyfile(file: "${project.jastadd.genResDir}/JavaSupportLevel.properties") {
					entry(key: "javaVersion", value: JastAddModule.get(project.jastadd.module).javaVersion())
				}
			}
		}

		project.task("cleanGen", type: Delete) {
			delete { project.jastadd.genDir }
			delete { project.jastadd.genResDir }
		}

		project.clean.dependsOn 'cleanGen'
		project.compileJava.dependsOn 'generateJava'
		project.processResources.dependsOn 'setSupportLevel'

		project.extensions.create("jastadd", JastAddExtension)
	}

}

class JastAddExtension {
	/**
	 * Load modules.
	 */
	void modules(project, list) {
		list.each { ModuleLoader.load(project, it) }
	}

	/** module name */
	String module

	/** supported Java version */
	String javaVersion

	String astPackage
	String sourceDir
	String toolsDir
	String resourceDir
	String binDir
	String tmpDir
	String genDir
	String genResDir
}

