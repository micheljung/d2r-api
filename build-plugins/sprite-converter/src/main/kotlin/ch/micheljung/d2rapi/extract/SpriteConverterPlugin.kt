package ch.micheljung.d2rapi.extract

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import java.nio.file.Path

interface SpriteConverterPluginExtension {
  val source: DirectoryProperty
  val target: DirectoryProperty
}

class SpriteConverterPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("d2rSpriteConverter", SpriteConverterPluginExtension::class.java)

    target.tasks.register("sprites", SpriteConverterTask::class.java) { task ->
      task.dataFolder.set(extension.source)
      task.extractFolder.set(extension.target)
    }
  }
}