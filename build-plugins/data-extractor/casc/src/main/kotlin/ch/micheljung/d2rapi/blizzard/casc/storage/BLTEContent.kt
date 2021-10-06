package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import ch.micheljung.d2rapi.lang.Hex
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BLTE content entry, used to decode BLTE file data that follows it.
 */
class BLTEContent(blteBuffer: ByteBuffer) {
  val compressedSize: Long
  val decompressedSize: Long
  private val hash = ByteArray(HASH_LENGTH)
  override fun toString(): String {
    val builder = StringBuilder()
    builder.append("BLTEChunk{compressedSize=")
    builder.append(compressedSize)
    builder.append(", decompressedSize=")
    builder.append(decompressedSize)
    builder.append(", hash=")
    Hex.stringBufferAppendHex(builder, hash)
    builder.append("}")
    return builder.toString()
  }

  fun getHash(): ByteArray {
    return hash.clone()
  }

  companion object {
    /**
     * BLTE content identifier.
     */
    private val IDENTIFIER = ByteBuffer.wrap(byteArrayOf('B'.toByte(), 'L'.toByte(), 'T'.toByte(), 'E'.toByte()))

    /**
     * Hash length in bytes. Should be fetched from appropriate digest length.
     */
    private const val HASH_LENGTH = 16

    fun decodeContent(storageBuffer: ByteBuffer): Array<BLTEContent> {
      val contentBuffer = storageBuffer.slice()

      // check identifier
      if (contentBuffer.remaining() < IDENTIFIER.remaining() || contentBuffer.limit(IDENTIFIER.remaining()) != IDENTIFIER) {
        throw MalformedCascStructureException("Missing BLTE identifier")
      }

      // decode header
      contentBuffer.limit(contentBuffer.capacity())
      contentBuffer.position(contentBuffer.position() + IDENTIFIER.remaining())
      contentBuffer.order(ByteOrder.BIG_ENDIAN)
      val headerSize = try {
        Integer.toUnsignedLong(contentBuffer.int)
      } catch (e: BufferUnderflowException) {
        throw MalformedCascStructureException("Header preamble goes out of bounds")
      }
      if (headerSize == 0L) {
        storageBuffer.position(storageBuffer.position() + contentBuffer.position())
        return emptyArray()
      } else if (headerSize > contentBuffer.capacity()) {
        throw MalformedCascStructureException("BLTE header extends beyond storage buffer bounds")
      }
      contentBuffer.limit(headerSize.toInt())
      val blteBuffer = contentBuffer.slice()
      blteBuffer.order(ByteOrder.BIG_ENDIAN)
      contentBuffer.position(contentBuffer.limit())
      contentBuffer.limit(contentBuffer.capacity())
      val flags: Byte
      val entryCount: Int
      try {
        flags = blteBuffer.get()
        if (flags.toInt() != 0xF) {
          throw MalformedCascStructureException("Unknown flags")
        }
        // BE24 read
        val be24Bytes = 3
        val be24Buffer = ByteBuffer.allocate(Integer.BYTES)
        be24Buffer.order(ByteOrder.BIG_ENDIAN)
        blteBuffer[be24Buffer.array(), Integer.BYTES - be24Bytes, be24Bytes]
        entryCount = be24Buffer.getInt(0)
        if (entryCount == 0) {
          throw MalformedCascStructureException("Explicit zero entry count")
        }
      } catch (e: BufferUnderflowException) {
        throw MalformedCascStructureException("Header goes out of bounds")
      }

      val content = Array(entryCount) {
        BLTEContent(blteBuffer)
      }

      if (blteBuffer.hasRemaining()) {
        throw MalformedCascStructureException("Unprocessed BLTE bytes")
      }
      storageBuffer.position(storageBuffer.position() + contentBuffer.position())
      return content
    }
  }

  init {
    compressedSize = Integer.toUnsignedLong(blteBuffer.int)
    decompressedSize = Integer.toUnsignedLong(blteBuffer.int)
    blteBuffer[hash]
  }
}