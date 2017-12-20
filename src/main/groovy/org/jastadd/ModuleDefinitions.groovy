package org.jastadd

/** Delegate object for JastAdd module definitions. */
class ModuleDefinitions {
	final File directory
	final ModuleLoader loader

	ModuleDefinitions(ModuleLoader loader, File dir) {
		this.loader = loader
		this.directory = dir
	}

	/* Declares a module. */
	def module(name, closure) {
		def module = new JastAddModule(name)
		module.basedir = directory.path
		module.loader = loader
		loader.addModule module
		closure.delegate = module
		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure()
		module
	}

	/*
	 * Includes another module file.
	 */
	def include(String moduleFile) {
		loader.load(directory.toPath().resolve(moduleFile).toFile())
	}
}
