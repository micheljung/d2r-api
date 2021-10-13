package ch.micheljung.d2rapi.dto

import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.streams.toList

class ResolversGenerator(private val logger: Logger) {

  private val csv = Csv()
  private val json = Json()

  fun generate(sourcePath: Path, targetPath: Path) {
    Files.createDirectories(targetPath)

    val resolvers = Files.list(sourcePath).use { stream ->
      stream.map {
        logger.info("Processing $it")
        val baseName = it.fileName.baseName()
        val items = csv.read(it)

        val targetFile = targetPath.resolve("${baseName}.mjs")

        logger.info("Writing $targetFile")

        val content = """
          |import flexsearch from 'flexsearch';
          |const { Document } = flexsearch;
          |const $baseName = new Document({
          |  cache: 1000,
          |  store: true,
          |  index: [
          |${items[0].keys.joinToString(prefix = "    \"", postfix = "\"", separator = "\",\n    \"")}
          |  ]
          |});
          |
          |${
          items.mapIndexed { index: Int, map: Map<*, *> ->
            // TODO convert values to guessed type
            "$baseName.add($index, ${json.asString(map)})"
          }.joinToString("\n")
        }
          |
          |export {$baseName};
          """.trimMargin()
        Files.writeString(
          targetFile, content,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.CREATE
        )
        targetFile
      }.toList()
    }

    Files.writeString(
      targetPath.resolve("index.mjs"), """
      |${
        resolvers.map {
          val importName = it.fileName.baseName()
          "import {$importName} from './${it.fileName}'"
        }.joinToString("\n")
      }
      |export const root = {
      |${
        resolvers.map {
          val queryName = it.fileName.baseName()
          val importName = it.fileName.baseName()
          "  $queryName: (args) => { return [...new Set($importName.search(args.query, Object.assign({enrich: true}, args.options)).map(it => it.result.map(it2=>it2.doc)).flat())] },"
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