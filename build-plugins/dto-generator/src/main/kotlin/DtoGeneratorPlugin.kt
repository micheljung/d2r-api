package ch.micheljung.d2rapi.dto

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

interface DtoGeneratorPluginExtension {
  /** The directory containing the directory structure to the extracted excel files. */
  val source: DirectoryProperty

  /** Where to write the DTOs. */
  val target: DirectoryProperty
}

class DtoGeneratorPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("d2rDtoGenerator", DtoGeneratorPluginExtension::class.java)

    target.tasks.register("generate", GeneratorTask::class.java) { task ->
      task.dataFolder.set(extension.source)
      task.dtoFolder.set(extension.target)
      task.dependsOn("extract")
    }

    target.plugins.apply("ch.micheljung.d2rapi.data-extractor")
  }
}