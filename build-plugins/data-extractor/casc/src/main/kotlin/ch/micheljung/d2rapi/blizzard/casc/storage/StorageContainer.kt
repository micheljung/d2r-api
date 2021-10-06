package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.nio.HashMismatchException
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High level storage container representing a storage entry.
 */
class StorageContainer(storageBuffer: ByteBuffer) {
  /**
   * Container encoding key.
   */
  var key: Key? = null
  var size: Long = 0
  var flags: Short = 0
  override fun toString(): String {
    return "FileEntry{key=" +
      key +
      ", size=" +
      size +
      ", flags=" +
      Integer.toBinaryString(flags.toInt()) +
      "}"
  }

  companion object {
    /**
     * Size of storage encoding key in bytes.
     */
    private const val ENCODING_KEY_SIZE = 16
  }

  init {
    val containerBuffer = storageBuffer.slice()
    containerBuffer.order(ByteOrder.LITTLE_ENDIAN)

    // key is in reversed byte order
    val checksumA: Int
    val checksumB: Int
    try {
      val keyArray = ByteArray(ENCODING_KEY_SIZE)
      val keyEnd = containerBuffer.position() + keyArray.size
      var writeIndex = 0
      var readIndex = keyEnd - 1
      while (writeIndex < keyArray.size) {
        keyArray[writeIndex] = containerBuffer[readIndex]
        writeIndex += 1
        readIndex -= 1
      }
      containerBuffer.position(keyEnd)
      key = Key(keyArray)
      size = Integer.toUnsignedLong(containerBuffer.int)
      flags = containerBuffer.short
      checksumA = containerBuffer.int
      checksumB = containerBuffer.int
    } catch (e: BufferUnderflowException) {
      throw MalformedCascStructureException("Storage buffer too small")
    }
    if (checksumA != checksumA) {
      throw HashMismatchException("Container checksum A mismatch")
    }
    if (checksumB != checksumB) {
      throw HashMismatchException("Container checksum B mismatch")
    }
    storageBuffer.position(storageBuffer.position() + containerBuffer.position())
  }
}