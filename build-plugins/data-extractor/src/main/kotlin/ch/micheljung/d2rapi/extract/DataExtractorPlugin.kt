package ch.micheljung.d2rapi.extract

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import java.nio.file.Path

interface DataExtractorPluginExtension {
  val source: DirectoryProperty
  val target: DirectoryProperty
  val excludes: ListProperty<String>
}

class DataExtractorPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("d2rExtractor", DataExtractorPluginExtension::class.java)

    target.tasks.register("extract", DataExtractorTask::class.java) { task ->
      task.dataFolder.set(extension.source)
      task.extractFolder.set(extension.target)
      task.excludes.set(extension.excludes)
    }
  }
}