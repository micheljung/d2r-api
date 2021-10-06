package ch.micheljung.d2rapi.blizzard.casc

import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import ch.micheljung.d2rapi.nio.ByteBufferInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * File containing CASC configuration information. This is basically a
 * collection of keys with their assigned value. What the values mean depends on
 * the purpose of the key.
 */
class ConfigurationFile(fileBuffer: ByteBuffer) {

    /** Underlying map holding the configuration data. */
    val configuration: MutableMap<String, String> = HashMap()

    companion object {
        /** The name of the data folder containing the configuration files. */
        private const val CONFIGURATION_FOLDER_NAME = "config"

        /** Character encoding used by configuration files. */
        private val FILE_ENCODING: Charset = StandardCharsets.UTF_8

        /** Length of the configuration bucket folder names. */
        private const val BUCKET_NAME_LENGTH = 2

        /** Number of configuration bucket folder tiers. */
        private const val BUCKET_TIERS = 2

        /**
         * Retrieve a configuration file from the data folder by its key.
         *
         * @param dataFolder Path of the CASC data folder.
         * @param keyHex     Key for configuration file as a hexadecimal string.
         * @return The requested configuration file.
         * @throws IOException If an exception occurs when retrieving the file.
         */
        fun lookupConfigurationFile(
            dataFolder: Path,
            keyHex: String
        ): ConfigurationFile {
            var file =
                dataFolder.resolve(CONFIGURATION_FOLDER_NAME)
            var tier = 0
            while (tier < BUCKET_TIERS) {
                val keyOffset = tier * BUCKET_NAME_LENGTH
                val bucketFolderName = keyHex.substring(
                    keyOffset,
                    keyOffset + BUCKET_NAME_LENGTH
                )
                file = file.resolve(bucketFolderName)
                tier += 1
            }
            file = file.resolve(keyHex)
            val fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file))
            return ConfigurationFile(fileBuffer)
        }
    }

    /**
     * Construct a configuration file by decoding a file buffer.
     *
     * @param fileBuffer File buffer to decode from.
     * @throws IOException If one or more IO errors occur.
     */
    init {
        ByteBufferInputStream(fileBuffer).use { fileStream ->
            Scanner(
                fileStream,
                FILE_ENCODING
            ).use { lineScanner ->
                while (lineScanner.hasNextLine()) {
                    val line = lineScanner.nextLine().trim { it <= ' ' }
                    val lineLength = line.indexOf('#')
                    val record: String = if (lineLength != -1) {
                        line.substring(0, lineLength)
                    } else {
                        line
                    }
                    if (record != "") {
                        val assignmentIndex = record.indexOf('=')
                        if (assignmentIndex == -1) {
                            throw MalformedCascStructureException(
                                "Configuration file line contains record with no assignment"
                            )
                        }
                        val key = record.substring(0, assignmentIndex).trim { it <= ' ' }
                        val value = record.substring(assignmentIndex + 1).trim { it <= ' ' }
                        if (configuration.putIfAbsent(key, value) != null) {
                            throw MalformedCascStructureException(
                                "Configuration file contains duplicate key declarations"
                            )
                        }
                    }
                }
            }
        }
    }
}