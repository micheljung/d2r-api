package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.nio.HashMismatchException
import ch.micheljung.d2rapi.blizzard.casc.nio.LittleHashBlockProcessor
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class IndexFile(fileBuffer: ByteBuffer) {
  var bucketIndex = 0
    private set
  var encodingKeyLength = 0
    private set
  private var dataFileSizeBits = 0
  var dataSizeMaximum: Long = 0
    private set
  private val entries = ArrayList<IndexEntry>()

  private fun decode(fileBuffer: ByteBuffer) {
    val sourceBuffer = fileBuffer.slice()

    // decode header
    val hashBlockProcessor = LittleHashBlockProcessor()
    val headerBuffer: ByteBuffer
    headerBuffer = try {
      hashBlockProcessor.getBlock(sourceBuffer)
    } catch (e: HashMismatchException) {
      throw MalformedCascStructureException("Header block corrupt", e)
    }
    headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val fileSizeLength: Int
    val dataOffsetLength: Int
    try {
      if (headerBuffer.short.toInt() != 7) {
        // possibly malformed
      }
      bucketIndex = java.lang.Byte.toUnsignedInt(headerBuffer.get())
      if (headerBuffer.get().toInt() != 0) {
        // possibly malformed
      }
      fileSizeLength = java.lang.Byte.toUnsignedInt(headerBuffer.get())
      dataOffsetLength = java.lang.Byte.toUnsignedInt(headerBuffer.get())
      encodingKeyLength = java.lang.Byte.toUnsignedInt(headerBuffer.get())
      dataFileSizeBits = java.lang.Byte.toUnsignedInt(headerBuffer.get())
      dataSizeMaximum = headerBuffer.long
    } catch (e: BufferUnderflowException) {
      throw MalformedCascStructureException("Header block too small")
    }

    // decode entries
    val entriesAlignmentMask = ENTRY_BLOCK_ALIGNMENT - 1
    sourceBuffer.position(sourceBuffer.position() + entriesAlignmentMask and entriesAlignmentMask.inv())
    val entryBuffer: ByteBuffer
    entryBuffer = try {
      hashBlockProcessor.getBlock(sourceBuffer)
    } catch (e: HashMismatchException) {
      throw MalformedCascStructureException("entries block corrupt", e)
    }
    val entryLength = fileSizeLength + dataOffsetLength + encodingKeyLength
    val entryCount = entryBuffer.remaining() / entryLength
    entries.ensureCapacity(entryCount)
    val decodeDataOffsetBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    val decodeDataOffsetOffset = java.lang.Long.BYTES - dataOffsetLength
    val decodeFileSizeBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    decodeFileSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
    var i = 0
    while (i < entryCount) {
      val key = ByteArray(encodingKeyLength)
      entryBuffer[key]
      entryBuffer[decodeDataOffsetBuffer.array(), decodeDataOffsetOffset, dataOffsetLength]
      val dataOffset = decodeDataOffsetBuffer.getLong(0)
      entryBuffer[decodeFileSizeBuffer.array(), 0, fileSizeLength]
      val fileSize = decodeFileSizeBuffer.getLong(0)

      // this can be used to detect special cross linking entries
      // if (getIndexNumber(entry.key, entry.key.length) != bucketIndex);
      // System.out.println("Bad key index: index=" + i + ", entry=" + entry + ",
      // bucket=" + getIndexNumber(entry.key, entry.key.length));
      entries.add(IndexEntry(key, dataOffset, fileSize))
      i += 1
    }
    if (entryBuffer.hasRemaining()) {
      throw MalformedCascStructureException("Unable to fully process entries block")
    }
    fileBuffer.position(fileBuffer.position() + sourceBuffer.position())
  }

  fun getStoreIndex(dataOffset: Long): Int {
    return (dataOffset ushr dataFileSizeBits).toInt()
  }

  fun getStoreOffset(dataOffset: Long): Long {
    return dataOffset and (1L shl dataFileSizeBits) - 1L
  }

  fun getEntry(encodingKey: Key): IndexEntry? {
    val index = Collections.binarySearch(entries, encodingKey) { left: Any?, right: Any? ->
      if (left is IndexEntry && right is Key) {
        return@binarySearch left.key.compareTo(right)
      }
      throw IllegalArgumentException("Binary search comparing in inverted order")
    }
    return if (index >= 0) entries[index] else null
  }

  fun getEntry(index: Int): IndexEntry {
    return entries[index]
  }

  val entryCount: Int
    get() = entries.size

  companion object {
    /**
     * Alignment of the index entry block in bytes.
     */
    private const val ENTRY_BLOCK_ALIGNMENT = 16
  }

  init {
    decode(fileBuffer)
  }
}