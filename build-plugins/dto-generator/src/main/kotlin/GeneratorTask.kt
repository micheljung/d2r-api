package ch.micheljung.d2rapi.dto

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GeneratorTask : DefaultTask() {

  init {
    group = "d2r"
  }

  @get:InputDirectory
  abstract val dataFolder: DirectoryProperty

  @get:OutputDirectory
  abstract val dtoFolder: DirectoryProperty

  @TaskAction
  fun generate() {
    val dataFolderPath = dataFolder.get().asFile.toPath()
    val source = dataFolderPath.resolve("data/data/global/excel")
    val target = dtoFolder.get().asFile.toPath()

    DtoGenerator(logger).generate(source, target)
    ServiceGenerator(logger).generate(source, target, dataFolderPath)
    ResourceGenerator(logger).generate(source, target)
  }
}