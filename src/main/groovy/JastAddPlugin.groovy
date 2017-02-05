import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*

class JastAddPlugin implements Plugin<Project> {

	void apply(Project project) {
		def jastadd = project.extensions.create('jastadd', JastAddExtension, project)

		project.configurations.create('jastadd2')

		project.repositories {
			mavenCentral()
		}

		project.dependencies {
			jastadd2 'org.jastadd:jastadd:2.2.2'
		}
	}

}

class ScannerConfig {
	/** The name of the generated scanner (default="Scanner"). */
	String name = 'Scanner'

	/** Directory to generate scanner in. */
	String genDir
}

class ParserConfig {
	/** Generated parser name (default="Parser"). */
	String name = 'Parser'

	/** Directory to generate parser in. */
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
		println 'Configuring JastAdd'
		loader = new ModuleLoader(this)
		this.project = project
	}

	void configureModuleBuild() {
		project.configurations.create('jastaddParser')
		project.configurations.create('jflex')
		project.configurations.create('beaver')

		project.dependencies {
			jastaddParser 'org.jastadd:jastaddparser:1.0.3'
			jastaddParser 'net.sf.beaver:beaver-rt:0.9.11'
			jflex 'de.jflex:jflex:1.6.1'
			beaver 'net.sf.beaver:beaver-ant:0.9.11'
		}

		project.sourceSets.main.java.srcDir { genDir }

		project.task('bashBuild') << {
			description 'Generates a Bash script to build this project.'
			def scannerFiles = project.files(
				module.files(project, 'scanner')
			)
			def parserFiles = project.files(
				module.files(project, 'parser')
			)
			def jastaddFiles = project.files(
				module.files(project, 'jastadd')
			)
			def scannerDir = scanner.genDir ?: "${genDir}/scanner"
			def parserDir = parser.genDir ?: "${genDir}/parser"
			def outdir = genDir
			def relpath = { path ->
				project.projectDir.toURI().relativize(path.toURI()).toString()
			}
			project.file('build.sh').withWriter { writer ->
				writer.writeLine '#!/bin/bash'
				writer.writeLine 'set -eu'
				writer.writeLine 'source config.sh # Configure the build with this file.'
				writer.writeLine 'mkdir -p "build/tmp"'

				// Generate scanner.
				writer.writeLine 'echo "Generating scanner..."'
				writer.write 'cat'
				scannerFiles.each { writer.write " \\\n    '${relpath(it)}'" }
				writer.writeLine " \\\n    > \"build/tmp/${scanner.name}.flex\""
				writer.writeLine "mkdir -p \"${scannerDir}\""
				writer.writeLine "\${JFLEX} -d \"${scannerDir}\" --nobak \"build/tmp/${scanner.name}.flex\""

				// Generate parser.
				writer.writeLine 'echo "Generating parser..."'
				writer.write 'cat'
				parserFiles.each { writer.write " \\\n    '${relpath(it)}'" }
				writer.writeLine " \\\n    > \"build/tmp/${parser.name}.all\""
				writer.writeLine "\${JASTADDPARSER} \"build/tmp/${parser.name}.all\" \"build/tmp/${parser.name}.beaver\""
				writer.writeLine "mkdir -p \"${parserDir}\""
				writer.writeLine "\${BEAVER} -d \"${parserDir}\" -t -c -w \"build/tmp/${parser.name}.beaver\""

				// Generate Java code with JastAdd.
				writer.writeLine 'echo "Generating node types and weaving aspects..."'

				writer.writeLine "mkdir -p \"${outdir}\""
				writer.writeLine "\${JASTADD} \\"
				writer.writeLine "    --package=\"${astPackage}\" \\"
				writer.writeLine "    --o=\"${outdir}\" \\"
				writer.writeLine '    --rewrite=cnta \\'
				writer.writeLine '    --safeLazy \\'
				writer.writeLine '    --beaver \\'
				writer.writeLine '    --visitCheck=false \\'
				writer.write     '    --cacheCycle=false'
				jastaddFiles.each { writer.write " \\\n    '${relpath(it)}'" }
				writer.write ' ${EXTRA_JASTADD_SOURCES}'
				writer.writeLine ''

				// Compile the generated code.
				writer.writeLine 'echo "Compiling Java code..."'
				writer.writeLine 'mkdir -p build/classes/main'
				writer.writeLine 'javac -d build/classes/main $(find src/java -name \'*.java\') \\'
				writer.writeLine '    $(find src/gen -name \'*.java\') \\'
				writer.writeLine '    $(find extendj/src/frontend -name \'*.java\') ${EXTRA_JAVA_SOURCES}'
				writer.writeLine 'mkdir -p src/gen-res'
				def date = new Date()
				writer.writeLine "echo \"moduleName: ${module.moduleName()}\" > src/gen-res/BuildInfo.properties"
				writer.writeLine "echo \"moduleVariant: ${module.moduleVariant()}\" >> src/gen-res/BuildInfo.properties"
				writer.writeLine "echo \"timestamp: ${date.format("yyyy-MM-dd'T'HH:mm'Z'")}\" >> src/gen-res/BuildInfo.properties"
				writer.writeLine "echo \"build.date: ${date.format("yyyy-MM-dd")}\" >> src/gen-res/BuildInfo.properties"
				writer.writeLine "jar cef \"${project.jar.manifest.attributes.get('Main-Class')}\" \"${project.name}.jar\" \\"
				writer.writeLine '    -C build/classes/main . \\'
				writer.writeLine '    -C src/gen-res BuildInfo.properties \\'
				writer.writeLine '    -C extendj/src/res Version.properties'
			}
		}

		project.task('generateJava', type: JavaExec, dependsOn: [ 'scanner', 'parser' ]) {
			description 'Generates Java sources from JastAdd code.'

			inputs.files { moduleSources + module.files(project, 'jastadd') }
			outputs.dir { project.file(genDir) }

			classpath = project.configurations.jastadd2
			main = 'org.jastadd.JastAdd'

			doFirst {
				def outdir = project.file(genDir)
				outdir.mkdirs()
				args ([ '--rewrite=cnta',
					'--safeLazy',
					'--beaver',
					"--package=${astPackage}",
					'--visitCheck=false',
					'--cacheCycle=false',
					"--o=${outdir.path}" ]
					+ extraJastAddOptions
					+ module.files(project, 'jastadd'))
			}
		}

		project.task('scanner', type: JavaExec) {
			description 'Generates scanner with JFlex.'

			inputs.files { moduleSources + module.files(project, 'scanner') }
			outputs.dir {
				// This needs to be a closure so that the genDir configuration variable can be used.
				project.file(scanner.genDir ?: "${genDir}/scanner")
			}

			classpath = project.configurations.jflex
			main = 'jflex.Main'

			doFirst {
				def inputFiles = project.files(module.files(project, 'scanner'))
				def outdir = project.file(scanner.genDir ?: "${genDir}/scanner")
				outdir.mkdirs()
				ant.concat(destfile: "${temporaryDir}/${scanner.name}.flex",
					binary: true, force: false) {
					inputFiles.addToAntBuilder(ant, "fileset", FileCollection.AntType.FileSet)
				}
				args ([ '-d', outdir.path, "${temporaryDir}/${scanner.name}.flex" ])
			}
		}

		project.task('preprocessParser', type: JavaExec) {
			description 'Generates Beaver parser with JastAddParser.'

			inputs.files { moduleSources + module.files(project, 'parser') }
			outputs.file {
				project.file("${temporaryDir}/${parser.name}.beaver")
			}

			classpath = project.configurations.jastaddParser
			main = 'org.jastadd.jastaddparser.Main'

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

		project.task('parser', type: JavaExec, dependsOn: [ 'preprocessParser' ]) {
			description 'Generates parser with Beaver.'

			inputs.files {
				project.file("${project.preprocessParser.temporaryDir}/${parser.name}.beaver")
			}
			outputs.dir {
				// This needs to be a closure so that the genDir configuration variable can be used.
				project.file(parser.genDir ?: "${genDir}/parser")
			}

			classpath = project.configurations.beaver
			main = 'beaver.comp.run.Make'

			doFirst {
				def outdir = project.file(parser.genDir ?: "${genDir}/parser")
				outdir.mkdirs()
				def inputFile = project.file("${project.preprocessParser.temporaryDir}/${parser.name}.beaver")
				args ([ '-d', outdir.path, '-t', '-c', '-w', inputFile.path ])
			}
		}

		// Only if buildInfoDir is not null:
		project.task('buildInfo') {
			description 'Generates a property file with the module name.'
			outputs.dir { buildInfoDir ? project.file(buildInfoDir) : null }
			doLast {
				if (buildInfoDir) {
					def date = new Date()
					ant.mkdir dir: "${buildInfoDir}"
					ant.propertyfile(file: "${buildInfoDir}/BuildInfo.properties") {
						entry(key: 'moduleName', value: module.moduleName())
						entry(key: 'moduleVariant', value: module.moduleVariant())
						entry(key: 'timestamp', value: date.format("yyyy-MM-dd'T'HH:mm'Z'"))
						entry(key: 'build.date', value: date.format('yyyy-MM-dd'))
					}
				}
			}
		}

		project.task('cleanGen', type: Delete) {
			description 'Removes generated files.'
			delete {
				def dirs = [
					scanner.genDir,
					parser.genDir,
					buildInfoDir,
					genDir
				]
				dirs.removeAll([null])
				dirs
			}
		}

		project.clean.dependsOn 'cleanGen'
		project.compileJava.dependsOn 'generateJava'
		project.processResources.dependsOn 'buildInfo'
	}

	/** Load module specifications. */
	void modules(String... modules) {
		modules.each { loader.load(project, it) }
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
	String genDir
	String buildInfoDir
	List<String> extraJastAddOptions = [];
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
