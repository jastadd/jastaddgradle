class ModuleDefinitions {
	def project
	String directory = ""

	ModuleDefinitions(project, dir) {
		directory = dir
		this.project = project
	}

	/* declare a module */
	def module(name, closure) {
		def module = new JastAddModule(name)
		module.basedir = directory
		closure.delegate = module
		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure()
		module
	}

	/* include modules from another directory */
	def include(moduleDir) {
		ModuleLoader.load(project, "${directory}/${moduleDir}")
	}
}

