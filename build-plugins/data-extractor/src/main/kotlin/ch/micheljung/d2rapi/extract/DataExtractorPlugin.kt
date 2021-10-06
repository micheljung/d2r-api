package ch.micheljung.d2rapi.extract

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

interface DataExtractorPluginExtension {
  val source: DirectoryProperty
  val target: DirectoryProperty
}

class DataExtractorPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("d2rExtractor", DataExtractorPluginExtension::class.java)

    target.tasks.register("extract", DataExtractorTask::class.java) { task ->
      task.dataFolder.set(extension.source)
      task.extractFolder.set(extension.target)
    }
  }
}