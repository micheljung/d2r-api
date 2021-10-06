package ch.micheljung.d2rapi.extract

import ch.micheljung.d2rapi.blizzard.casc.ConfigurationFile
import ch.micheljung.d2rapi.blizzard.casc.storage.Storage
import ch.micheljung.d2rapi.blizzard.casc.vfs.VirtualFileSystem
import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.function.Predicate
import kotlin.streams.toList

class ExcelDataExtractor(
  private val dataFolder: Path,
  private val extractFolder: Path,
  private val logger: Logger,
) {

  fun extract() {
    val buildInfo = readBuildInfo()
    val buildKey = buildInfo.getBuildKey()
    val buildConfiguration = ConfigurationFile.lookupConfigurationFile(dataFolder, buildKey)

    Storage(dataFolder, useOld = false, useMemoryMapping = true).use { storage ->
      logger.debug("Mounting VFS")
      val vfs = VirtualFileSystem(storage, buildConfiguration.configuration)

      logger.debug("Getting all paths")
      val allFilePaths = vfs.getAllFiles()

      allFilePaths
        .filter { it.path.startsWith("data\\data\\global\\excel\\") && it.path.endsWith(".txt") }
        .forEach { pathResult ->
          val filePath = pathResult.path
          val outputPath = extractFolder.resolve(filePath)
          val fileSize = pathResult.fileSize
          val exists = pathResult.existsInStorage()

          if (exists && !pathResult.isTvfs) {
            Files.createDirectories(outputPath.parent)

            BufferedReader(InputStreamReader(ByteArrayInputStream(pathResult.readFile().array()))).use { reader ->
              val content = reader.lines()
                .filter(getFilterForFile(String(pathResult.pathFragments.last()!!, StandardCharsets.UTF_8)))
                .toList()
                .joinToString("\r\n")

              if (String(pathResult.pathFragments.last()!!, StandardCharsets.UTF_8).endsWith("magicsuffix.txt")) {
                System.err.println(content)
              }

              Files.writeString(
                outputPath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
              )
            }
            vfs.resolvePath(pathResult.pathFragments)
          }
          if (exists)
            logger.debug("Extracting $filePath ($fileSize B)")
          else
            logger.warn("$filePath ($fileSize B) does not exist")
        }

      logger.debug("Files successfully extracted")
    }
  }

  private fun getFilterForFile(path: String): Predicate<in String> = when (path) {
    "uniqueitems.txt" -> Predicate {
      val trimmed = it.trim()
      trimmed != "Expansion" &&
        trimmed != "Armor" &&
        trimmed != "Elite Uniques" &&
        trimmed != "Rings" &&
        trimmed != "Class Specific"
    }

    "overlay.txt"
    -> Predicate { !it.startsWith("EXPANSION") }

    "magicprefix.txt",
    "magicsuffix.txt",
    "monseq.txt",
    "monsounds.txt",
    "montype.txt",
    "sounds.txt",
    "treasureclassex.txt",
    -> Predicate { !it.startsWith("\t") }

    else -> Predicate { !it.startsWith("Expansion\t") && it.trim() != "Expansion" }
  }

  private fun readBuildInfo(): ch.micheljung.d2rapi.blizzard.casc.info.BuildInfo {
    val infoFile =
      dataFolder.parent.resolve(ch.micheljung.d2rapi.blizzard.casc.info.BuildInfo.Companion.BUILD_INFO_FILE_NAME)
    logger.debug("Reading $infoFile")
    return ch.micheljung.d2rapi.blizzard.casc.info.BuildInfo.Companion.read(infoFile)
  }
}