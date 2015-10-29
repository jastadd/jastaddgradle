import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*

class JastAddPlugin implements Plugin<Project> {

	void apply(Project project) {
		def jastadd = project.extensions.create("jastadd", JastAddExtension, project)

		project.configurations.create('jastadd2')
		project.configurations.create('jastaddParser')
		project.configurations.create('jflex')
		project.configurations.create('beaver')

		project.repositories {
			mavenCentral()
		}

		project.dependencies {
			jastadd2 group: 'org.jastadd', name: 'jastadd', version: '2.1.13'
			jastaddParser group: 'org.jastadd', name: 'jastaddparser', version: '1.0.3'
			jastaddParser group: 'net.sf.beaver', name: 'beaver-rt', version: '0.9.11'
			jflex group: 'de.jflex', name: 'jflex', version: '1.4.3'
			beaver group: 'net.sf.beaver', name: 'beaver-ant', version: '0.9.11'
			compile group: 'net.sf.beaver', name: 'beaver-rt', version: '0.9.11'
		}

		project.sourceSets.main.java.srcDir { jastadd.genDir }

		project.task("generateJava", dependsOn: [ "scanner", "parser" ]) {
			description 'generate Java sources from JastAdd aspects'
			inputs.files { jastadd.moduleSources + jastadd.module.files(project, "jastadd") }
			outputs.dir { project.file(jastadd.genDir) }
			doLast {
				def outdir = jastadd.genDir
				def specFiles = project.files(
					jastadd.module.files(project, "jastadd")
				)
				ant.mkdir(dir: project.file(outdir))
				ant.taskdef(name: "jastadd", classname: "org.jastadd.JastAddTask",
					classpath: project.configurations.jastadd2.asPath) { }
				ant.jastadd(
					package: jastadd.astPackage,
					rewrite: 'regular',
					beaver: true,
					visitCheck: false,
					cacheCycle: false,
					outdir: project.file(outdir),
					defaultMap: "new org.jastadd.util.RobustMap(new java.util.HashMap())") {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
			}
		}

		project.task("scanner") {
			description 'generate JFlex scanner'
			inputs.files { jastadd.moduleSources + jastadd.module.files(project, "scanner") }
			outputs.dir {
				def outdir = jastadd.scanner.genDir ?: "${jastadd.genDir}/scanner"
				project.file(outdir)
			}
			doLast {
				def specFiles = project.files(
					jastadd.module.files(project, "scanner")
				)
				ant.concat(destfile: "${temporaryDir}/JavaScanner.flex",
					binary: true, force: false) {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				ant.mkdir(dir: "${jastadd.genDir}/scanner")
				ant.taskdef(name: "jflex", classname: "JFlex.anttask.JFlexTask",
					classpath: project.configurations.jflex.asPath)
				def outdir = jastadd.scanner.genDir ?: "${jastadd.genDir}/scanner"
				ant.mkdir(dir: project.file(outdir))
				ant.jflex(file: "${temporaryDir}/JavaScanner.flex",
					outdir: project.file(outdir),
					nobak: true)
			}
		}

		project.task("parser") {
			description 'generate Beaver parser'
			inputs.files { jastadd.moduleSources + jastadd.module.files(project, "parser") }
			outputs.dir {
				def outdir = jastadd.parser.genDir ?: "${jastadd.genDir}/parser"
				project.file(outdir)
			}
			doLast {
				def specFiles = project.files(
					jastadd.module.files(project, "parser")
				)
				def parserName = jastadd.parser.name
				ant.concat(destfile: "${temporaryDir}/${parserName}.all",
					binary: true, force: false) {
					specFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				ant.java(classname: 'org.jastadd.jastaddparser.Main', fork: true) {
					classpath {
						pathelement(path: project.configurations.jastaddParser.asPath)
					}
					arg(value: "${temporaryDir}/${parserName}.all")
					arg(value: "${temporaryDir}/${parserName}.beaver")
				}
				ant.mkdir(dir: "${jastadd.genDir}/parser")
				ant.taskdef(name: "beaver", classname: "beaver.comp.run.AntTask",
					classpath: project.configurations.beaver.asPath)
				def outdir = jastadd.parser.genDir ?: "${jastadd.genDir}/parser"
				ant.mkdir(dir: project.file(outdir))
				ant.beaver(file: "${temporaryDir}/${parserName}.beaver",
					destdir: outdir,
					terminalNames: true,
					compress: false,
					useSwitch: true)
			}
		}

		// Only if jastadd.buildInfoDir is not null.
		project.task("buildInfo") {
			description 'generate a property file with the module name'
			outputs.dir { jastadd.buildInfoDir ? project.file(jastadd.buildInfoDir) : null }
			doLast {
				if (jastadd.buildInfoDir) {
					def date = new Date()
					ant.mkdir dir: "${jastadd.buildInfoDir}"
					ant.propertyfile(file: "${jastadd.buildInfoDir}/BuildInfo.properties") {
						entry(key: 'moduleName', value: jastadd.module.moduleName())
						entry(key: 'moduleVariant', value: jastadd.module.moduleVariant())
						entry(key: 'timestamp', value: date.format("yyyy-MM-dd'T'HH:mm'Z'"))
						entry(key: 'build.date', value: date.format('yyyy-MM-dd'))
					}
				}
			}
		}

		project.task("cleanGen", type: Delete) {
			description 'remove generated files'
			delete {
				def dirs = [
					jastadd.scanner.genDir,
					jastadd.parser.genDir,
					jastadd.buildInfoDir,
					jastadd.genDir
				]
				dirs.removeAll([null])
				dirs
			}
		}

		project.clean.dependsOn 'cleanGen'
		project.compileJava.dependsOn 'generateJava'
		project.processResources.dependsOn 'buildInfo'
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
	final ModuleLoader loader

	/** All loaded modules. */
	List modules = []

	/** All module sources. */
	List moduleSources = []

	JastAddExtension(Project project) {
		loader = new ModuleLoader(this)
		this.project = project
	}

	/** load module specifications */
	void modules(String... modules) {
		modules.each { loader.load(project, it) }
	}

	/** module instance */
	private JastAddModule module

	/** set the target module */
	public void setModule(String name) {
		if (module != null) {
			throw new InvalidUserDataException("Target module already selected!")
		}
		module = getModule(name)
		// add Java sources included in modules to source set
		project.compileJava.source project.files(module.files(project, 'java')),
			project.sourceSets.main.java
	}

	/** get the target module instance */
	public JastAddModule getModule() {
		module
	}

	public void addModuleSource(source) {
		moduleSources.add source
	}

	/** supported Java version */
	String javaVersion

	String astPackage
	String genDir
	String buildInfoDir
	ScannerConfig scanner = new ScannerConfig()
	ParserConfig parser = new ParserConfig()

	public void addModule(module) {
		modules.add module
	}

	def getModule(name) {
		for (module in modules) {
			if (module.name == name) {
				return module
			}
		}
		throw new InvalidUserDataException("Unknown module ${name}")
	}

}

