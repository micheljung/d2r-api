package ch.micheljung.d2rapi.blizzard.casc.vfs

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

/**
 * Decodes file data for file value nodes.
 *
 *
 * This is done by collating together data. First the file is resolved from the
 * file buffer then the data describing its contents is resolved from the
 * contents buffer.
 */
class TVFSDecoder {
  private var encodingKeySize = 0
  private var contentsOffsetSize = 0
  private var pathBuffer: ByteBuffer? = null
  private var logicalBuffer: ByteBuffer? = null
  private var storageBuffer: ByteBuffer? = null

  /**
   * The offset into the content buffer is a special type that uses the minimum
   * number of bytes to hold the largest offset. Hence non-standard types such as
   * 3 bytes long big-endian integer are possible so a special buffer is needed to
   * decode these numbers.
   */
  private val contentsOffsetDecoder: ByteBuffer = ByteBuffer.allocate(Integer.BYTES)

  fun decodeContainer(): List<PathNode> {
    return decodeContainer(pathBuffer)
  }

  private fun decodeContainer(pathBuffer: ByteBuffer?): List<PathNode> {
    val nodes = ArrayList<PathNode>()
    while (pathBuffer!!.hasRemaining()) {
      val node = decodeNode(pathBuffer)
      nodes.add(node)
    }
    return nodes
  }

  private fun decodeNode(pathBuffer: ByteBuffer?): PathNode {
    val pathFragments = ArrayList<ByteArray>()
    val node: PathNode
    try {
      var pathStringLength: Int
      while (java.lang.Byte.toUnsignedInt(pathBuffer!!.get())
          .also { pathStringLength = it } != VALUE_PATH_STRING_LENGTH
      ) {
        val pathFragment = ByteArray(pathStringLength)
        pathBuffer[pathFragment]
        pathFragments.add(pathFragment)
      }
      val value = pathBuffer.int
      node = if (value and VALUE_CONTAINER_FLAG != 0) {
        // prefix node
        val containerSize = value and VALUE_CONTAINER_FLAG.inv()
        pathBuffer.position(pathBuffer.position() - Integer.BYTES)
        if (containerSize > pathBuffer.remaining()) {
          throw MalformedCascStructureException("Prefix node container extends beyond path container")
        }
        pathBuffer.limit(pathBuffer.position() + containerSize)
        val containerBuffer = pathBuffer.slice()
        pathBuffer.position(pathBuffer.limit())
        pathBuffer.limit(pathBuffer.capacity())
        containerBuffer.position(Integer.BYTES)
        val nodes = decodeContainer(containerBuffer)
        PrefixNode(pathFragments, nodes)
      } else {
        // file value
        val fileReferences = getFileReferences(value)
        FileNode(pathFragments, fileReferences)
      }
    } catch (e: BufferUnderflowException) {
      throw MalformedCascStructureException("Path stream goes beyond path container")
    }
    return node
  }

  private fun getFileReferences(fileOffset: Int): Array<StorageReference> {
    if (fileOffset > logicalBuffer!!.limit()) {
      throw MalformedCascStructureException("Logical offset beyond file reference chunk")
    }
    logicalBuffer!!.position(fileOffset)

    try {
      val referenceCount = java.lang.Byte.toUnsignedInt(logicalBuffer!!.get())
      return Array(referenceCount) {
        val offset = Integer.toUnsignedLong(logicalBuffer!!.int)
        val size = Integer.toUnsignedLong(logicalBuffer!!.int)
        logicalBuffer!![contentsOffsetDecoder.array(), Integer.BYTES - contentsOffsetSize, contentsOffsetSize]
        val cascReferenceOffset = contentsOffsetDecoder.getInt(0)
        if (cascReferenceOffset > storageBuffer!!.limit()) {
          throw MalformedCascStructureException("Storage offset beyond CASC reference chunk")
        }
        storageBuffer!!.position(cascReferenceOffset)
        try {
          val encodingKeyDecoder = ByteArray(encodingKeySize)
          storageBuffer!![encodingKeyDecoder]
          val physicalSize = storageBuffer!!.int
          val unknownMember1 = storageBuffer!!.get()
          val actualSize = storageBuffer!!.int
          val reference = StorageReference(
            offset, size, Key(encodingKeyDecoder), physicalSize,
            unknownMember1, actualSize
          )
          reference
        } catch (e: BufferUnderflowException) {
          throw MalformedCascStructureException("Storage goes out of bounds")
        }
      }
    } catch (e: BufferUnderflowException) {
      throw MalformedCascStructureException("Logical reference goes out of bounds")
    }
  }

  fun loadFile(fileBuffer: ByteBuffer): TVFSFile {
    val localBuffer = fileBuffer.slice()

    // check identifier
    if (localBuffer.remaining() < IDENTIFIER.remaining() || localBuffer.limit(IDENTIFIER.remaining()) != IDENTIFIER) {
      throw MalformedCascStructureException("Missing TVFS identifier")
    }

    // decode header
    localBuffer.limit(localBuffer.capacity())
    localBuffer.position(IDENTIFIER.remaining())
    val maximumPathDepth: Int
    val cascReferenceSize: Int
    val cascReferenceOffset: Int
    val fileReferenceSize: Int
    val fileReferenceOffset: Int
    val pathSize: Int
    val pathOffset: Int
    val patchKeySize: Int
    val flags: Int
    val version: Byte
    try {
      version = localBuffer.get()
      if (version.toInt() != 1) {
        throw UnsupportedOperationException("Unsupported TVFS version: $version")
      }
      val headerSize = java.lang.Byte.toUnsignedInt(localBuffer.get())
      if (headerSize > localBuffer.capacity()) {
        throw MalformedCascStructureException("TVFS header extends past end of file")
      }
      localBuffer.limit(headerSize)
      encodingKeySize = java.lang.Byte.toUnsignedInt(localBuffer.get())
      patchKeySize = java.lang.Byte.toUnsignedInt(localBuffer.get())
      flags = localBuffer.int
      pathOffset = localBuffer.int
      pathSize = localBuffer.int
      if (Integer.toUnsignedLong(pathOffset) + Integer.toUnsignedLong(pathSize) > localBuffer.capacity()) {
        throw MalformedCascStructureException("Path stream extends past end of file")
      }
      fileReferenceOffset = localBuffer.int
      fileReferenceSize = localBuffer.int
      if (Integer.toUnsignedLong(fileReferenceOffset) + Integer.toUnsignedLong(fileReferenceSize) > localBuffer.capacity()) {
        throw MalformedCascStructureException("Logical data extends past end of file")
      }
      cascReferenceOffset = localBuffer.int
      cascReferenceSize = localBuffer.int
      if (Integer.toUnsignedLong(cascReferenceOffset) + Integer.toUnsignedLong(cascReferenceSize) > localBuffer.capacity()) {
        throw MalformedCascStructureException("Storage data extends past end of file")
      }
      maximumPathDepth = java.lang.Short.toUnsignedInt(localBuffer.short)
    } catch (e: BufferUnderflowException) {
      throw MalformedCascStructureException("Header goes out of bounds")
    }
    contentsOffsetSize =
      Math.max(1, Integer.BYTES - Integer.numberOfLeadingZeros(cascReferenceSize) / java.lang.Byte.SIZE)
    contentsOffsetDecoder.putInt(0, 0)
    localBuffer.limit(pathOffset + pathSize)
    localBuffer.position(pathOffset)
    pathBuffer = localBuffer.slice()
    localBuffer.clear()
    localBuffer.limit(fileReferenceOffset + fileReferenceSize)
    localBuffer.position(fileReferenceOffset)
    logicalBuffer = localBuffer.slice()
    localBuffer.clear()
    localBuffer.limit(cascReferenceOffset + cascReferenceSize)
    localBuffer.position(cascReferenceOffset)
    storageBuffer = localBuffer.slice()
    localBuffer.clear()
    val rootNodes = decodeContainer()
    return TVFSFile(version, flags, encodingKeySize, patchKeySize, maximumPathDepth, rootNodes)
  }

  companion object {
    /** TVFS file identifier located at start of TVFS files.  */
    private val IDENTIFIER = ByteBuffer.wrap(byteArrayOf('T'.toByte(), 'V'.toByte(), 'F'.toByte(), 'S'.toByte()))

    /**
     * Flag for container values. If set inside a value then the value is a
     * container of other nodes otherwise it is a file.
     */
    private const val VALUE_CONTAINER_FLAG = -0x80000000

    /**
     * Specifier for path node value. If path string length is this then value
     * follows.
     */
    private const val VALUE_PATH_STRING_LENGTH = 0xFF
  }

}