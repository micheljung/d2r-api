package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Main data storage of a CASC archive. It consists of index files which point
 * to storage containers in data files.
 */
class Storage(dataFolder: Path, useOld: Boolean, private val useMemoryMapping: Boolean) : AutoCloseable {
  private val folder: Path = dataFolder.resolve(DATA_FOLDER_NAME)
  private val channelMap = HashMap<Int, FileChannel?>()
  private val indicies = arrayOfNulls<IndexFile>(INDEX_COUNT)

  /**
   * Used to track closed status of the store.
   */
  private var closed = false
  private val encodingKeyLength: Int

  @Synchronized
  override fun close() {
    if (closed) {
      return
    }
    var exception: IOException? = null
    for ((_, value) in channelMap) {
      try {
        value!!.close()
      } catch (e: IOException) {
        if (exception != null) {
          exception.addSuppressed(e)
        } else {
          exception = e
        }
      }
    }
    closed = true
    if (exception != null) {
      throw IOException("IOExceptions occurred during closure", exception)
    }
  }

  fun hasBanks(encodingKey: Key): Boolean {
    val bucketIndex = getBucketIndex(encodingKey.key, encodingKeyLength)
    val index = indicies[bucketIndex]
    val indexEntry = index!!.getEntry(encodingKey)
    return indexEntry != null
  }

  fun getBanks(encodingKey: Key): BankStream {
    val bucketIndex = getBucketIndex(encodingKey.key, encodingKeyLength)
    val index = indicies[bucketIndex]
    val indexEntry =
      index!!.getEntry(encodingKey) ?: throw FileNotFoundException("Encoding key '$encodingKey' not in store indices")
    val dataOffset = indexEntry.dataOffset
    val storeIndex = index.getStoreIndex(dataOffset)
    val storeOffset = index.getStoreOffset(dataOffset)
    val storageBuffer = getStorageBuffer(storeIndex, storeOffset, indexEntry.fileSize)
    return BankStream(storageBuffer, indexEntry.key)
  }

  @Synchronized
  private fun getDataFileChannel(index: Int): FileChannel? {
    if (closed) {
      throw ClosedChannelException()
    }
    var fileChannel = channelMap[index]
    if (fileChannel == null) {
      if (index > DATA_FILE_INDEX_MAXIMUM) {
        throw MalformedCascStructureException("Storage data file index too large")
      }
      val builder = StringBuilder()
      builder.append(DATA_FILE_NAME)
      builder.append('.')
      val extensionNumber = Integer.toUnsignedString(index)
      val extensionZeroCount = DATA_FILE_EXTENSION_LENGTH - extensionNumber.length
      builder.append("0".repeat(Math.max(0, extensionZeroCount)))
      builder.append(extensionNumber)
      val filePath = folder.resolve(builder.toString())
      fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)
      channelMap[index] = fileChannel
    }
    return fileChannel
  }

  /**
   * Fetch a buffer from storage.
   *
   * @param index  Data file index.
   * @param offset Data file offset.
   * @param length Requested buffer length.
   * @return Storage buffer.
   * @throws IOException If a problem occurs when preparing the storage buffer.
   */
  private fun getStorageBuffer(index: Int, offset: Long, length: Long): ByteBuffer {
    val fileChannel = getDataFileChannel(index)
    if (length > Int.MAX_VALUE) {
      throw MalformedCascStructureException("Data buffer too large to process")
    }
    val storageBuffer: ByteBuffer
    if (useMemoryMapping) {
      val mappedBuffer = fileChannel!!.map(MapMode.READ_ONLY, offset, length)
      mappedBuffer.load()
      storageBuffer = mappedBuffer
    } else {
      storageBuffer = ByteBuffer.allocate(length.toInt())
      while (storageBuffer.hasRemaining() && fileChannel!!.read(
          storageBuffer,
          offset + storageBuffer.position()
        ) != -1
      );
      if (storageBuffer.hasRemaining()) {
        throw EOFException("Unexpected end of file")
      }
      storageBuffer.clear()
    }
    return storageBuffer
  }

  /**
   * Loads a file fully into memory. Memory mapping is used if allowed.
   *
   * @param file Path of file to load into memory.
   * @return File buffered into memory.
   * @throws IOException If an IO exception occurs.
   */
  private fun loadFileFully(file: Path?): ByteBuffer {
    val fileBuffer: ByteBuffer
    FileChannel.open(file, StandardOpenOption.READ).use { channel ->
      val fileLength = channel.size()
      if (fileLength > Int.MAX_VALUE) {
        throw MalformedCascStructureException("File too large to process")
      }
      if (useMemoryMapping) {
        val mappedBuffer = channel.map(MapMode.READ_ONLY, 0, fileLength)
        mappedBuffer.load()
        fileBuffer = mappedBuffer
      } else {
        fileBuffer = ByteBuffer.allocate(fileLength.toInt())
        while (fileBuffer.hasRemaining() && channel.read(fileBuffer, fileBuffer.position().toLong()) != -1);
        if (fileBuffer.hasRemaining()) {
          throw EOFException("Unexpected end of file")
        }
        fileBuffer.clear()
      }
    }
    return fileBuffer
  }

  companion object {
    /**
     * The name of the data folder containing the configuration files.
     */
    const val DATA_FOLDER_NAME = "data"

    /**
     * Number of index files used by a data store.
     */
    private const val INDEX_COUNT = 16

    /**
     * Usual number of copies of a specific index located in the folder. This is an
     * estimate only used to increase search performance and will not effect
     * results.
     */
    private const val INDEX_COPIES = 2

    /**
     * File extension used by storage index files.
     */
    const val INDEX_FILE_EXTENSION = "idx"

    /**
     * File name of data files. 3 character extension is the index.
     */
    const val DATA_FILE_NAME = "data"

    /**
     * Largest permitted data file index.
     */
    const val DATA_FILE_INDEX_MAXIMUM = 999

    /**
     * Extension length used by data files. Defined by the length needed to store
     * DATA_FILE_INDEX_MAXIMUM as a decimal string.
     */
    const val DATA_FILE_EXTENSION_LENGTH = 3

    /**
     * Converts an encoding key into an index file number.
     *
     * @param encodingKey Input encoding key.
     * @param keyLength   Length of key to be processed.
     * @return Index number.
     */
    fun getBucketIndex(encodingKey: ByteArray, keyLength: Int): Int {
      var accumulator = 0
      var i = 0
      while (i < keyLength) {
        accumulator = accumulator xor encodingKey[i].toInt()
        i += 1
      }
      val nibbleMask = (1 shl 4) - 1
      return accumulator and nibbleMask xor (accumulator shr 4) and nibbleMask
    }
  }

  /**
   * Construct a storage object from the provided data folder.
   *
   *
   * Using memory mapping should give the best performance. However some platforms
   * or file systems might not support it.
   *
   * @param dataFolder       Path of the CASC data folder.
   * @param useOld           Use other (old?) version of index files.
   * @param useMemoryMapping If IO should be memory mapped.
   * @throws IOException If there was a problem loading from the data folder.
   */
  init {
    val indexFiles = ArrayList<Path>(INDEX_COUNT * INDEX_COPIES)
    Files.newDirectoryStream(folder, "*." + INDEX_FILE_EXTENSION).use { indexFileIterator ->
      for (indexFile in indexFileIterator) {
        indexFiles.add(indexFile)
      }
    }
    class IndexFileNameMeta {
      var filePath: Path? = null
      var index = 0
      var version: Long = 0
    }

    val metaMap = HashMap<Int, ArrayList<IndexFileNameMeta>>(INDEX_COUNT)
    for (indexFile in indexFiles) {
      val fileName = indexFile.fileName.toString()
      val fileMeta = IndexFileNameMeta()
      fileMeta.filePath = indexFile
      fileMeta.index = Integer.parseUnsignedInt(fileName.substring(0, 2), 16)
      fileMeta.version = java.lang.Long.parseUnsignedLong(fileName.substring(2, 10), 16)
      val bucketList = metaMap.computeIfAbsent(fileMeta.index) { k: Int? -> ArrayList() }
      bucketList.add(fileMeta)
    }
    var bucketOrder: Comparator<IndexFileNameMeta> =
      Comparator { left: IndexFileNameMeta, right: IndexFileNameMeta -> (left.version - right.version).toInt() }
    if (!useOld) {
      bucketOrder = Collections.reverseOrder(bucketOrder)
    }
    run {
      var index = 0
      while (index < indicies.size) {
        val bucketList = metaMap[index] ?: throw MalformedCascStructureException("Storage index file missing")
        bucketList.sortWith(bucketOrder)
        val fileMeta = bucketList[0]
        // Index file versions loaded. Possibly useful for debugging
        val idxVersions = LongArray(INDEX_COUNT)
        idxVersions[index] = fileMeta.version
        indicies[index] = IndexFile(loadFileFully(fileMeta.filePath))
        index += 1
      }
    }

    // resolve index key length being used
    var index = 0
    encodingKeyLength = indicies[index++]!!.encodingKeyLength
    while (index < indicies.size) {
      if (encodingKeyLength != indicies[index]!!.encodingKeyLength) {
        throw MalformedCascStructureException("Inconsistent encoding key length between index files")
      }
      index += 1
    }
  }
}