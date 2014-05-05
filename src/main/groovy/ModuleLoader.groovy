class ModuleLoader {
	static void load(project, dir) {
		def name = "modules"
		def source = project.file("${dir}/${name}");
		if (!source.exists()) {
			throw new Error("Could not load module definitions: ${dir}/${name}")
		}
		def code = source.text
		def closure = new GroovyShell().evaluate("{->${code}}")
		closure.delegate = new ModuleDefinitions(dir)
		closure()
	}

}
