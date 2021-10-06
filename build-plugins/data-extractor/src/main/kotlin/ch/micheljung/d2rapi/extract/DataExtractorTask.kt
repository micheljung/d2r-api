package ch.micheljung.d2rapi.extract

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class DataExtractorTask : DefaultTask() {

  init {
    group = "d2r"
  }

  @get:InputDirectory
  abstract val dataFolder: DirectoryProperty

  @get:OutputDirectory
  abstract val extractFolder: DirectoryProperty

  @TaskAction
  fun extract() {
    val source = dataFolder.get().asFile.toPath()
    val target = extractFolder.get().asFile.toPath()

    logger.info("Extracting $source to $target")

    ExcelDataExtractor(source, target, logger).extract()
  }
}
