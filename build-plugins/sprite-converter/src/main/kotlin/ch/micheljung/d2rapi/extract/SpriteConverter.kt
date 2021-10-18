package ch.micheljung.d2rapi.extract

import com.google.common.io.LittleEndianDataInputStream
import org.gradle.api.logging.Logger
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

@Suppress("UnstableApiUsage")
class ExcelSpriteConverter(
  private val source: Path,
  private val targetFolder: Path,
  private val logger: Logger,
) {

  fun convert() {
    Files.walk(source).use { stream ->
      stream.forEach {
        if (Files.isDirectory(it)) {
          return@forEach
        }
        if (!it.fileName.toString().endsWith(".sprite")) {
          return@forEach
        }

        val readAllBytes = Files.readAllBytes(it)


        logger.info("Processing: $it")

        LittleEndianDataInputStream(ByteArrayInputStream(readAllBytes)).use { dataStream ->
          val header = String(dataStream.readNBytes(4), StandardCharsets.ISO_8859_1)

          val version = dataStream.readUnsignedShort()
          val frameWidth = dataStream.readUnsignedShort()

          val width = dataStream.readInt()
          val height = dataStream.readInt()
          dataStream.skip(4)
          val frames = dataStream.readInt()
          dataStream.skip(8)
          val streamSize = dataStream.readInt()
          dataStream.skip(4)

          if (version != 31) {
            logger.warn("Can't process $it because version is: $version")
            return@forEach
          }

          (0 until frames).forEach { frame ->
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            (0 until height).forEach { y ->
              (0 until frameWidth).forEach { x ->
                val r = dataStream.readUnsignedByte()
                val g = dataStream.readUnsignedByte()
                val b = dataStream.readUnsignedByte()
                val a = dataStream.readUnsignedByte()

                image.setRGB(x, y, Color(r, g, b, a).rgb)
              }
            }

            val fileName = it.fileName.toString().replace(".sprite", ".png")
            val targetFile = targetFolder.resolve(source.relativize(it)).resolveSibling(fileName)
            Files.createDirectories(targetFile.parent)
            logger.info("Writing: $targetFile")
            Files.deleteIfExists(targetFile)
            ImageIO.write(image, "png", targetFile.toFile())
          }
        }

//        var header = ""
//        header = header + chr(int(bytes.read(8).hex, 16))
//        header = header + chr(int(bytes.read(8).hex, 16))
//        header = header + chr(int(bytes.read(8).hex, 16))
//        header = header + chr(int(bytes.read(8).hex, 16))
      }
    }
  }
}