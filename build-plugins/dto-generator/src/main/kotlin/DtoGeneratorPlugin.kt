package ch.micheljung.d2rapi.dto

import ch.micheljung.d2rapi.extract.DataExtractorPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

interface DtoGeneratorPluginExtension {
  /** The Diablo 2: Resurrected Data directory. */
  val source: DirectoryProperty

  /** Where to write the DTOs. */
  val target: DirectoryProperty
}

class DtoGeneratorPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("d2rDtoGenerator", DtoGeneratorPluginExtension::class.java)

    target.tasks.register("generate", GeneratorTask::class.java) { task ->
      val dataExtractFolder = target.buildDir.resolve("resources/main")

      task.dataFolder.set(dataExtractFolder)
      task.dtoFolder.set(extension.target)

      target.plugins.withId("ch.micheljung.d2rapi.data-extractor") {
        target.extensions.configure<DataExtractorPluginExtension>("d2rExtractor") {
          it.source.set(extension.source.get())
          it.target.set(dataExtractFolder)
        }
      }

      task.dependsOn("extract")
    }

    target.plugins.apply("ch.micheljung.d2rapi.data-extractor")
  }
}