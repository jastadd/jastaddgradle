package org.jastadd

import org.gradle.api.InvalidUserDataException

class ModuleLoader {
	final JastAddExtension extension

	public ModuleLoader(JastAddExtension extension) {
		this.extension = extension;
	}

	void load(project, moduleName) {
		def source = project.file(moduleName)
		def dir = source.parent
		if (source.isDirectory()) {
			source = project.file("${moduleName}/modules")
			dir = moduleName
		}
		if (!source.exists()) {
			throw new InvalidUserDataException("Could not load module definitions: ${moduleName}")
		}
		extension.addModuleSource source
		def code = source.text
		def closure = new GroovyShell().evaluate("{->${code}}")
		closure.delegate = new ModuleDefinitions(this, project, dir)
		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure()
	}

	/** Track the loaded module. */
	public void addModule(module) {
		extension.addModule module
	}

	/** Lookup an existing module. */
	def get(module) {
		extension.getModule module
	}
}
