package ch.micheljung.d2rapi.dto

import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.streams.toList

class ResolversGenerator(private val logger: Logger) {

  private val csv2Json = Csv2Json()

  fun generate(sourcePath: Path, targetPath: Path) {
    Files.createDirectories(targetPath)

    val resolvers = Files.list(sourcePath).use { stream ->
      stream.map {
        logger.info("Processing $it")
        val baseName = it.fileName.baseName()

        val json = csv2Json.convert(it)
        val targetFile = targetPath.resolve("${baseName}.mjs")

        logger.info("Writing $targetFile")
        Files.writeString(
          targetFile,
          "export default $json;",
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.CREATE
        )
        targetFile
      }.toList()
    }

    Files.writeString(
      targetPath.resolve("index.mjs"), """
      |${resolvers.map {
        val importName = it.fileName.baseName()
        "import $importName from './${it.fileName}';"
      }.joinToString("\n")}
      |
      |export const root = {
      |${
        resolvers.map {
          val queryName = it.fileName.baseName()
          "  $queryName: () => { return $queryName; },"
        }.joinToString("\n")
      }
      |};
      """.trimMargin()
    )

//    Files.write(
//      targetPath.resolve("index.mjs"), resolvers.map {
//        val exportAsName = it.fileName.baseName()
//        "export * as $exportAsName from './${exportAsName}'"
//      }
//    )
  }
}

fun Path.baseName() = fileName.toString().substringBeforeLast('.')