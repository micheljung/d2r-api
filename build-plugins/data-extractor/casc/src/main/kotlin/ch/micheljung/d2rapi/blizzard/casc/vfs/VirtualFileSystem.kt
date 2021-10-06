package ch.micheljung.d2rapi.blizzard.casc.vfs

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.StorageReference
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import ch.micheljung.d2rapi.blizzard.casc.storage.Storage
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.*
import java.util.regex.Pattern

/**
 * High level file system API using TVFS directories to extract files from a
 * store.
 */
class VirtualFileSystem(
  /**
   * Local CASC storage. Used to retrieve file data.
   */
  private val storage: Storage, buildConfiguration: Map<String, String>
) {
  /**
   * A result of a file path lookup operation in a TVFS file system.
   *
   *
   * Can be used to fetch the data of a file.
   */
  inner class PathResult
  /**
   * Internal constructor for path results.
   *
   * @param node          Resolved node.
   * @param pathFragments Path of resolved node.
   */(private val node: PathNode, val pathFragments: Array<ByteArray?>) {

    /**
     * Returns true if this file completely exists in storage.
     *
     *
     * The virtual file system structure lists all files, even ones that may not be
     * in storage. Only files that are in storage can have their file buffer read.
     *
     *
     * If this result is not a file then it exists in storage as it has no storage
     * footprint.
     *
     * @return True if the file exists in storage.
     */
    fun existsInStorage(): Boolean {
      var exists = true
      if (isFile) {
        val fileNode = node as FileNode
        val fileReferenceCount = fileNode.getFileReferenceCount()
        var fileReferenceIndex = 0
        while (fileReferenceIndex < fileReferenceCount) {
          val fileReference = fileNode.getFileReference(fileReferenceIndex)
          exists = exists && storage.hasBanks(fileReference.encodingKey)
          fileReferenceIndex += 1
        }
      }
      return exists
    }

    /**
     * Get the size of the file in bytes.
     *
     *
     * If this result is not a file a value of 0 is returned.
     *
     * @return File size in bytes.
     */
    val fileSize: Long
      get() {
        var size = 0L
        if (isFile) {
          val fileNode = node as FileNode
          val fileReferenceCount = fileNode.getFileReferenceCount()
          var fileReferenceIndex = 0
          while (fileReferenceIndex < fileReferenceCount) {
            val fileReference = fileNode.getFileReference(fileReferenceIndex)
            size = Math.max(size, fileReference.offset + fileReference.size)
            fileReferenceIndex += 1
          }
        }
        return size
      }

    @get:Throws(CharacterCodingException::class)
    val path: String
      get() = convertPathFragments(pathFragments)
    val isFile: Boolean
      get() = node is FileNode

    /**
     * Returns if this path result represents a TVFS file node used by this file
     * system.
     *
     *
     * Such nodes logically act as folders in the file path but also contain file
     * data used by this file system. Such behaviour may be incompatible with
     * standard file systems which do not support both a folder and file at the same
     * path.
     *
     *
     * Results that are not files cannot be a TVFS file.
     *
     * @return If this node is a TVFS file used by this file system.
     */
    val isTvfs: Boolean
      get() {
        if (!isFile) {
          return false
        }
        val fileNode = node as FileNode
        val fileReference = fileNode.getFileReference(0)
        return tvfsStorageReferences.containsKey(fileReference.encodingKey)
      }

    /**
     * Fully read this file into the specified destination buffer. If no buffer is
     * specified a new one will be allocated.
     *
     *
     * The specified buffer must have at least getFileSize bytes remaining.
     *
     * @param destBuffer Buffer to be written to.
     * @return Buffer that was written to.
     * @throws IOException      If an error occurs during reading.
     * @throws OutOfMemoryError If no buffer is specified and the file is too big
     * for a single buffer.
     */
    fun readFile(destBuffer: ByteBuffer = ByteBuffer.allocate(fileSize.toInt())): ByteBuffer {
      if (!isFile) {
        throw FileNotFoundException("Result is not a file")
      }
      val fileSize = fileSize
      if (fileSize > Int.MAX_VALUE) {
        throw OutOfMemoryError("File too big to process")
      }
      if (destBuffer.remaining() < fileSize) {
        throw BufferOverflowException()
      }
      val fileBuffer = destBuffer.slice()
      val fileNode = node as FileNode
      val fileReferenceCount = fileNode.getFileReferenceCount()
      var fileReferenceIndex = 0
      while (fileReferenceIndex < fileReferenceCount) {
        val fileReference = fileNode.getFileReference(fileReferenceIndex)
        val chunkSize = fileReference.size
        if (chunkSize != fileReference.actualSize) {
          throw MalformedCascStructureException("Bank and chunk size inconsistent")
        }
        val chunkOffset = fileReference.offset
        val bankStream = storage.getBanks(fileReference.encodingKey)
        // TODO test if compressed and logical sizes match stored sizes.
        fileBuffer.limit((chunkOffset + chunkSize).toInt())
        fileBuffer.position(chunkOffset.toInt())
        while (bankStream.hasNextBank()) {
          bankStream.getBank(fileBuffer)
        }
        fileReferenceIndex += 1
      }
      destBuffer.position(destBuffer.position() + fileSize.toInt())
      return destBuffer
    }
  }

  /**
   * Decoder used to load TVFS files in the TVFS tree.
   */
  private val decoder = TVFSDecoder()

  /**
   * TVFS file containing the root directory for the file system.
   */
  private val tvfsRoot: TVFSFile

  /**
   * TVFS file cache. Holds all loaded TVFS files for this file system. This
   * allows the TVFS files to be loaded lazily which could potentially reduce
   * loading times and memory usage when only some branches of the TVFS file tree
   * are accessed.
   */
  private val tvfsCache = TreeMap<Key, TVFSFile>()

  /**
   * Map of all TVFS files used by the TVFS file tree. Keys that are not in this
   * map are treated as leaf files rather than a nested TVFS file.
   */
  private val tvfsStorageReferences = TreeMap<Key, StorageReference>()

  /**
   * Resolves a TVFS storage reference into a data buffer from the local storage.
   *
   * @param storageReference TVFS storage reference.
   * @return Data buffer containing refered content.
   * @throws IOException If an exception occurs when fetching the data buffer.
   */
  private fun fetchStoredBuffer(storageReference: StorageReference): ByteBuffer {
    val size = storageReference.size
    if (size > Int.MAX_VALUE) {
      throw MalformedCascStructureException("Stored data too large to process")
    }
    val bankStream = storage.getBanks(storageReference.encodingKey)
    val storedBuffer = ByteBuffer.allocate(size.toInt())
    try {
      while (bankStream.hasNextBank()) {
        bankStream.getBank(storedBuffer)
      }
    } catch (e: BufferOverflowException) {
      throw MalformedCascStructureException("Stored data is bigger than expected")
    }
    if (storedBuffer.hasRemaining()) {
      throw MalformedCascStructureException("Stored data is smaller than expected")
    }
    storedBuffer.rewind()
    return storedBuffer
  }

  /**
   * Method to get all files in the file system.
   *
   * @return List of file path results for every file in the file system.
   * @throws IOException If an exception is thrown when loading a TVFS file or
   * decoding path fragments into a path string.
   */
  fun getAllFiles(): List<PathResult> {
    val pathStringList = ArrayList<PathResult>()
    val rootCount = tvfsRoot.getRootNodeCount()
    var rootIndex = 0
    while (rootIndex < rootCount) {
      val root = tvfsRoot.getRootNode(rootIndex)
      recursiveFilePathRetrieve(Array(1) { ByteArray(0) }, pathStringList, root)
      rootIndex += 1
    }
    return pathStringList
  }

  /**
   * Recursive function to traverse the TVFS tree and resolve all files in the
   * file system.
   *
   * @param parentPathFragments Path fragments of parent node.
   * @param resultList          Result list.
   * @param currentNode         The child node to process.
   * @throws IOException If an exception occurs when processing the node.
   */
  private fun recursiveFilePathRetrieve(
    parentPathFragments: Array<ByteArray?>,
    resultList: ArrayList<PathResult>,
    currentNode: PathNode
  ) {
    var currentPathFragments = parentPathFragments

    // process path fragments
    val fragmentCount = currentNode.getPathFragmentCount()
    if (fragmentCount > 0) {
      var fragmentIndex = 0
      val fragment = currentNode.getFragment(fragmentIndex++)

      // expand path fragment array
      var basePathFragmentsIndex = currentPathFragments.size
      if (fragmentCount > 1 || fragment.size > 0) {
        // first fragment of the node gets merged with last path fragment
        basePathFragmentsIndex -= 1
      }
      currentPathFragments = Arrays.copyOf(currentPathFragments, basePathFragmentsIndex + fragmentCount)

      // merge fragment
      val sourceFragment = currentPathFragments[basePathFragmentsIndex]
      var joinedFragment: ByteArray? = fragment
      if (sourceFragment != null) {
        joinedFragment = sourceFragment
        if (fragment.isNotEmpty()) {
          val joinOffset = sourceFragment.size
          joinedFragment = Arrays.copyOf(sourceFragment, joinOffset + fragment.size)
          System.arraycopy(fragment, 0, joinedFragment!!, joinOffset, fragment.size)
        }
      }

      // append path fragments
      currentPathFragments[basePathFragmentsIndex] = joinedFragment
      while (fragmentIndex < fragmentCount) {
        currentPathFragments[basePathFragmentsIndex + fragmentIndex] = currentNode.getFragment(fragmentIndex)
        fragmentIndex += 1
      }
    }
    if (currentNode is PrefixNode) {
      val prefixNode = currentNode
      val childCount = prefixNode.getNodeCount()
      var index = 0
      while (index < childCount) {
        recursiveFilePathRetrieve(currentPathFragments, resultList, prefixNode.getNode(index))
        index += 1
      }
    } else if (currentNode is FileNode) {
      val fileNode = currentNode
      val fileReferenceCount = fileNode.getFileReferenceCount()
      if (fileReferenceCount == 1) {
        // check if nested VFS
        val encodingKey = fileNode.getFileReference(0).encodingKey
        val tvfsFile = resolveTVFS(encodingKey)
        if (tvfsFile != null) {
          // file is also a folder
          val folderPathFragments = Arrays.copyOf(
            currentPathFragments,
            currentPathFragments.size + 1
          )
          folderPathFragments[currentPathFragments.size] = ByteArray(0)
          val rootCount = tvfsFile.getRootNodeCount()
          var rootIndex = 0
          while (rootIndex < rootCount) {
            val root = tvfsFile.getRootNode(rootIndex)
            recursiveFilePathRetrieve(folderPathFragments, resultList, root)
            rootIndex += 1
          }
        }
        resultList.add(PathResult(currentNode, currentPathFragments))
      }
    } else {
      throw IllegalArgumentException("Unsupported node type")
    }
  }

  /**
   * Recursive function to resolve a file node in a TVFS tree from path fragments
   * representing a file system file path.
   *
   * @param pathFragments  Path fragments of a file path.
   * @param fragmentIndex  Index of fragment where currently testing.
   * @param fragmentOffset Offset into fragment where currently testing.
   * @param node           Node which is being tested.
   * @return Resolved file node.
   * @throws IOException If an exception occurs when testing the node.
   */
  private fun recursiveResolvePathFragments(
    pathFragments: Array<ByteArray?>,
    fragmentIndex: Int,
    fragmentOffset: Int,
    node: PathNode
  ): FileNode? {
    var fragmentIndex = fragmentIndex
    var fragmentOffset = fragmentOffset
    if (!equalNodePathFragments(pathFragments, fragmentIndex, fragmentOffset, node)) {
      // node not on path
      return null
    }

    // advance fragment position
    val nodeFragmentCount = node.getPathFragmentCount()
    if (nodeFragmentCount == 1) {
      val nodeFragment = node.getFragment(0)
      if (nodeFragment.size == 0) {
        // node with termination fragment
        fragmentIndex += 1
        fragmentOffset = 0
      } else {
        // node with less than a whole fragment
        fragmentOffset += nodeFragment.size
      }
    } else if (nodeFragmentCount > 1) {
      // node which completes 1 or more fragments.
      fragmentIndex += nodeFragmentCount - 1
      fragmentOffset = node.getFragment(nodeFragmentCount - 1).size
    }

    // process node
    if (node is PrefixNode) {
      // apply binary search to prefix node to find next node
      val prefixNode = node
      val childCount = prefixNode.getNodeCount()
      var low = 0
      var high = childCount - 1
      while (low <= high) {
        val middle = (low + high) / 2
        val searchNode = prefixNode.getNode(middle)
        val result = compareNodePathFragments(pathFragments, fragmentIndex, fragmentOffset, searchNode)
        if (result == 0) {
          // possible match
          return recursiveResolvePathFragments(pathFragments, fragmentIndex, fragmentOffset, searchNode)
        } else if (result < 0) {
          high = middle - 1
        } else {
          low = middle + 1
        }
      }
    } else if (node is FileNode) {
      val fileNode = node
      if (fragmentIndex == pathFragments.size - 1
        && fragmentOffset == pathFragments[pathFragments.size - 1]!!.size
      ) {
        // file found
        return fileNode
      } else if (fragmentOffset == pathFragments[fragmentIndex]!!.size) {
        // nested TVFS file
        val fileReferenceCount = fileNode.getFileReferenceCount()
        if (fileReferenceCount == 1) {
          // check if nested VFS
          val encodingKey = fileNode.getFileReference(0).encodingKey
          val tvfsFile = resolveTVFS(encodingKey)
          if (tvfsFile != null) {
            // TVFS file to recursively resolve
            if (tvfsFile.getRootNodeCount() != 1) {
              throw MalformedCascStructureException("Logic only defined for 1 TVFS root node")
            }
            fragmentIndex += 1
            fragmentOffset = 0
            return recursiveResolvePathFragments(
              pathFragments, fragmentIndex, fragmentOffset,
              tvfsFile.getRootNode(0)
            )
          }
        }
      }
    } else {
      throw IllegalArgumentException("Unsupported node type")
    }

    // file not found
    return null
  }

  /**
   * Resolves a file from the specified path fragments representing a file system
   * file path.
   *
   * @param pathFragments File path fragments.
   * @return Path result for a file.
   * @throws FileNotFoundException If the file does not exist in the file system.
   * @throws IOException           If an exception occurs when resolving the path
   * fragments.
   */
  fun resolvePath(pathFragments: Array<ByteArray?>): PathResult {
    require(pathFragments.isNotEmpty()) { "pathFragments.length must be greater than 0" }
    if (tvfsRoot.getRootNodeCount() != 1) {
      throw MalformedCascStructureException("Logic only defined for 1 root node")
    }
    val result = recursiveResolvePathFragments(pathFragments, 0, 0, tvfsRoot.getRootNode(0))
      ?: throw FileNotFoundException("Path not in storage")
    return PathResult(result, pathFragments)
  }

  /**
   * Resolves a TVFS file from an encoding key. The key is checked if it is a TVFS
   * file in this file system and then resolved in local storage. The resulting
   * file is then decoded as a TVFS file and returned. Decoded TVFS files are
   * cached for improved performance. This method can be called concurrently.
   *
   * @param encodingKey Encoding key of TVFS file to resolve.
   * @return The resolved TVFS file, or null if the encoding key is not for a TVFS
   * file of this file system.
   * @throws IOException If an error occurs when resolving the TVFS file.
   */
  private fun resolveTVFS(encodingKey: Key): TVFSFile? {
    var tvfsFile: TVFSFile? = null
    val storageReference = tvfsStorageReferences[encodingKey]
    if (storageReference != null) {
      // is a TVFS file of this file system
      synchronized(this) {
        tvfsFile = tvfsCache[encodingKey]
        if (tvfsFile == null) {
          // decode TVFS from storage
          val rootBuffer = fetchStoredBuffer(storageReference)
          tvfsFile = decoder.loadFile(rootBuffer)
          tvfsCache[storageReference.encodingKey] = tvfsFile!!
        }
      }
    }
    return tvfsFile
  }

  companion object {
    /**
     * VFS storage reference key prefix.
     */
    const val CONFIGURATION_KEY_PREFIX = "vfs-"

    /**
     * Root VFS storage reference.
     */
    const val ROOT_KEY = "root"

    /**
     * Character encoding used internally by file paths.
     */
    val PATH_ENCODING = Charset.forName("UTF8")

    /**
     * Path separator used by path strings.
     */
    const val PATH_SEPERATOR = "\\"

    /**
     * Compares the path fragments of a node with a section of file path fragments.
     * This is useful for performing a binary search on a node's children.
     *
     *
     * A return value of 0 does not mean that the node is in the path fragments.
     * Only that if it were, it would be this node. This is because the children of
     * a node all have unique first fragment sequences so only the first fragment is
     * tested.
     *
     * @param pathFragments  Path fragments of a file path.
     * @param fragmentIndex  Index of fragment where to start comparing at.
     * @param fragmentOffset Offset into fragment to start comparing at.
     * @param node           Node which is being compared.
     * @return Similar to standard comparator value (see above).
     */
    private fun compareNodePathFragments(
      pathFragments: Array<ByteArray?>,
      fragmentIndex: Int,
      fragmentOffset: Int, node: PathNode
    ): Int {
      val nodeFragmentCount = node.getPathFragmentCount()
      if (nodeFragmentCount == 0) {
        // nodes without fragments have no path fragment presence so always match
        return 0
      }
      val nodeFragment = node.getFragment(0)
      val fragment = pathFragments[fragmentIndex]
      return if (nodeFragment.size == 0 && fragment!!.size - fragmentOffset > 0) {
        // node with termination fragment are always before all other child nodes
        1
      } else Arrays.compareUnsigned(
        fragment, fragmentOffset,
        Math.min(fragmentOffset + nodeFragment.size, fragment!!.size), nodeFragment, 0, nodeFragment.size
      )
    }

    /**
     * Convert a path string into path fragments for resolution in the VFS.
     *
     * @param filePath Path string to convert.
     * @return Path fragments.
     * @throws CharacterCodingException If the path string cannot be encoded into
     * fragments.
     */
    fun convertFilePath(filePath: String): Array<ByteArray?> {
      val fragmentStrings = filePath.toLowerCase(Locale.ROOT).split(Pattern.quote(PATH_SEPERATOR)).toTypedArray()
      val encoder = PATH_ENCODING.newEncoder()
      encoder.onMalformedInput(CodingErrorAction.REPORT)
      encoder.onUnmappableCharacter(CodingErrorAction.REPORT)
      var index = 0

      return fragmentStrings.map {
        val fragmentBuffer = encoder.encode(CharBuffer.wrap(fragmentStrings[index]))
        return@map if (fragmentBuffer.hasArray() && fragmentBuffer.limit() == fragmentBuffer.capacity() && fragmentBuffer.position() == 0) {
          // can use underlying array
          fragmentBuffer.array()
        } else {
          // copy into array
          val pathFragment = ByteArray(fragmentBuffer.remaining())
          fragmentBuffer[pathFragment]
          pathFragment
        }
      }.toTypedArray()
    }

    /**
     * Convert path fragments used internally by VFS into a path string.
     *
     * @param pathFragments Path fragments to convert.
     * @return Path string.
     * @throws CharacterCodingException If the path fragments cannot be decoded into
     * a valid String.
     */
    fun convertPathFragments(pathFragments: Array<ByteArray?>): String {
      val fragmentStrings = arrayOfNulls<String>(pathFragments.size)
      val decoder = PATH_ENCODING.newDecoder()
      decoder.onMalformedInput(CodingErrorAction.REPORT)
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
      var index = 0
      while (index < fragmentStrings.size) {
        fragmentStrings[index] = decoder.decode(ByteBuffer.wrap(pathFragments[index])).toString()
        index += 1
      }
      return java.lang.String.join(PATH_SEPERATOR, *fragmentStrings)
    }

    /**
     * Test the path fragments of a node form a section of file path fragments.
     *
     * @param pathFragments  Path fragments of a file path.
     * @param fragmentIndex  Index of fragment where to start testing at.
     * @param fragmentOffset Offset into fragment to start testing at.
     * @param node           Node which is being tested.
     * @return True if the node is contained in the path fragments, otherwise false.
     */
    private fun equalNodePathFragments(
      pathFragments: Array<ByteArray?>,
      fragmentIndex: Int,
      fragmentOffset: Int,
      node: PathNode
    ): Boolean {
      var innerFragmentOffset = fragmentOffset
      val nodeFragmentCount = node.getPathFragmentCount()
      if (nodeFragmentCount == 0) {
        // nodes without fragments have no path fragment presence so always match
        return true
      }
      if (nodeFragmentCount == 1 && node.getFragment(0).isEmpty()) {
        // node with termination fragment
        return innerFragmentOffset == pathFragments[fragmentIndex]!!.size
      } else if (pathFragments.size < fragmentIndex + nodeFragmentCount) {
        // fragment too short
        return false
      }
      var result = true
      var nodeFragmentIndex = 0
      while (result && nodeFragmentIndex < nodeFragmentCount) {
        val fragment = pathFragments[fragmentIndex + nodeFragmentIndex]
        val nodeFragment = node.getFragment(nodeFragmentIndex)
        result = result && Arrays.equals(
          fragment, innerFragmentOffset,
          Math.min(innerFragmentOffset + nodeFragment.size, fragment!!.size), nodeFragment, 0,
          nodeFragment.size
        )
        innerFragmentOffset = 0
        nodeFragmentIndex += 1
      }
      return result
    }
  }

  /**
   * Construct a TVFS file system from a CASC local storage and build
   * configuration.
   *
   * @param storage            CASC local storage to source files from.
   * @param buildConfiguration Build configuration of CASC archive.
   * @throws IOException If an exception occurs when loading the file system.
   */
  init {
    var vfsNumber = 0
    var configurationKey: String
    while (buildConfiguration
        .containsKey(
          (CONFIGURATION_KEY_PREFIX + Integer.toUnsignedString(++vfsNumber)).also { configurationKey = it })
    ) {
      val storageReference = StorageReference(configurationKey, buildConfiguration)
      tvfsStorageReferences[storageReference.encodingKey] = storageReference
    }
    val rootReference = StorageReference(CONFIGURATION_KEY_PREFIX + ROOT_KEY, buildConfiguration)
    val rootBuffer = fetchStoredBuffer(rootReference)
    tvfsRoot = decoder.loadFile(rootBuffer)
    tvfsCache[rootReference.encodingKey] = tvfsRoot
  }
}