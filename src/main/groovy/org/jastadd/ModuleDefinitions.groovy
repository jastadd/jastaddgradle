package org.jastadd

/** Delegate object for JastAdd module definitions. */
class ModuleDefinitions {
	def project
	String directory = ""
	final ModuleLoader loader

	ModuleDefinitions(ModuleLoader loader, project, dir) {
		this.loader = loader
		this.project = project
		this.directory = dir
	}

	/* Declares a module. */
	def module(name, closure) {
		def module = new JastAddModule(name)
		module.basedir = directory
		module.loader = loader
		loader.addModule module
		closure.delegate = module
		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure()
		module
	}

	/* Includes modules from another directory. */
	def include(moduleDir) {
		loader.load(project, "${directory}/${moduleDir}")
	}
}
