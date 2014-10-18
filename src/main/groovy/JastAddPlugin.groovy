import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*

class JastAddPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.configurations.create('jastadd')
		project.configurations.create('jastaddParser')
		project.configurations.create('jflex')
		project.configurations.create('beaver')

		project.dependencies {
			jastadd group: 'org.jastadd', name: 'jastadd2', version: '2.1.8'
			jastaddParser group: 'org.jastadd', name: 'JastAddParser', version: '1.0.2-17'
			jastaddParser group: 'net.sf.beaver', name: 'beaver-rt', version: '0.9.11'
			jflex group: 'de.jflex', name: 'jflex', version: '1.4.3'
			beaver group: 'net.sf.beaver', name: 'beaver-ant', version: '0.9.11'
			compile group: 'net.sf.beaver', name: 'beaver-rt', version: '0.9.11'
		}

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
					classpath: project.configurations.jastadd.asPath) { }
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
			outputs.dir {
				def outdir = project.jastadd.scanner.genDir ?: "${project.jastadd.genDir}/scanner"
				project.file(outdir)
			}
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
					classpath: project.configurations.jflex.asPath)
				def outdir = project.jastadd.scanner.genDir ?: "${project.jastadd.genDir}/scanner"
				ant.mkdir(dir: project.file(outdir))
				ant.jflex(file: "${project.jastadd.tmpDir}/scanner/JavaScanner.flex",
					outdir: project.file(outdir),
					nobak: true)
			}
		}

		project.task("parser") {
			inputs.files { JastAddModule.get(project.jastadd.module).files(project, "parser") }
			outputs.dir {
				def outdir = project.jastadd.parser.genDir ?: "${project.jastadd.genDir}/parser"
				project.file(outdir)
			}
			doLast {
				ant.mkdir(dir: project.file(project.jastadd.tmpDir))
				def specFiles = project.files(
					JastAddModule.get(project.jastadd.module).files(project, "parser")
				)
				def parserName = project.jastadd.parser.name
				ant.concat(destfile: "${project.jastadd.tmpDir}/parser/${parserName}.all",
					binary: true, force: false) {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				ant.java(classname: "Main", fork: true) {
					classpath {
						pathelement(path: project.configurations.jastaddParser.asPath)
					}
					arg(value: "${project.jastadd.tmpDir}/parser/${parserName}.all")
					arg(value: "${project.jastadd.tmpDir}/parser/${parserName}.beaver")
				}
				ant.mkdir(dir: "${project.jastadd.genDir}/parser")
				ant.taskdef(name: "beaver", classname: "beaver.comp.run.AntTask",
					classpath: project.configurations.beaver.asPath)
				def outdir = project.jastadd.parser.genDir ?: "${project.jastadd.genDir}/parser"
				ant.mkdir(dir: project.file(outdir))
				ant.beaver(file: "${project.jastadd.tmpDir}/parser/${parserName}.beaver",
					destdir: outdir,
					terminalNames: true,
					compress: false,
					useSwitch: true)
			}
		}

		project.task("setSupportLevel") {
			outputs.dir { project.jastadd.genResDir ? project.file(project.jastadd.genResDir) : null }
			doLast {
				if (project.jastadd.genResDir) {
					ant.mkdir dir: "${project.jastadd.genResDir}"
					ant.propertyfile(file: "${project.jastadd.genResDir}/JavaSupportLevel.properties") {
						entry(key: "javaVersion", value: JastAddModule.get(project.jastadd.module).javaVersion())
					}
				}
			}
		}

		project.task("cleanGen", type: Delete) {
			doLast {
				delete { project.jastadd.genDir }
				delete { project.jastadd.genResDir }
				if (project.jastadd.scanner.genDir)
					delete { project.jastadd.scanner.genDir }
				if (project.jastadd.parser.genDir)
					delete { project.jastadd.parser.genDir }
			}
		}

		project.clean.dependsOn 'cleanGen'
		project.compileJava.dependsOn 'generateJava'
		project.processResources.dependsOn 'setSupportLevel'

		project.extensions.create("jastadd", JastAddExtension, project)
	}

}

class ScannerConfig {
	/** Directory to generate scanner in */
	String genDir
}

class ParserConfig {
	/**
	 * Generated parser name (default="JavaParser")
	 */
	String name = "JavaParser"
	/** Directory to generate parser in */
	String genDir
}

class JastAddExtension {
	Project project

	JastAddExtension(Project project) {
		this.project = project
	}

	/**
	 * Load modules.
	 */
	void modules(String... modules) {
		modules.each { ModuleLoader.load(project, it) }
	}

	/** module name */
	String module

	/** supported Java version */
	String javaVersion

	String astPackage
	String sourceDir
	String tmpDir
	String genDir
	String genResDir
	ScannerConfig scanner = new ScannerConfig()
	ParserConfig parser = new ParserConfig()
}

