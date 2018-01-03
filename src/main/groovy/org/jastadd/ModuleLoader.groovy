package org.jastadd

import org.gradle.api.InvalidUserDataException

class ModuleLoader {
  final JastAddExtension extension

  public ModuleLoader(JastAddExtension extension) {
    this.extension = extension;
  }

  void load(String moduleFile) {
  }

  void load(File moduleFile) {
    if (!moduleFile.exists()) {
      throw new InvalidUserDataException(
          "Could not load module definitions from file: ${moduleFile}")
    }
    extension.addModuleSource moduleFile
    def code = moduleFile.text
    load(new GroovyShell().evaluate("{->${code}}"), moduleFile.parentFile)
  }

  void load(Closure closure, File basedir) {
    closure.delegate = new ModuleDefinitions(this, basedir)
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
