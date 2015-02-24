import org.gradle.api.InvalidUserDataException

class ModuleLoader {
	static void load(project, moduleName) {
		def source = project.file(moduleName)
		def dir = source.parent
		if (source.isDirectory()) {
			source = project.file("${moduleName}/modules")
			dir = moduleName
		}
		if (!source.exists()) {
			throw new InvalidUserDataException("Could not load module definitions: ${moduleName}")
		}
		def code = source.text
		def closure = new GroovyShell().evaluate("{->${code}}")
		closure.delegate = new ModuleDefinitions(project, dir)
		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure()
	}

}
