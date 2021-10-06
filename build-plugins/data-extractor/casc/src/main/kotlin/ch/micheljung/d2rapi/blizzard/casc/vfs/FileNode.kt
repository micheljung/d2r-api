package ch.micheljung.d2rapi.blizzard.casc.vfs

/** A file system node containing a logical file.  */
class FileNode(pathFragments: List<ByteArray>, private val references: Array<StorageReference>) :
  PathNode(pathFragments) {
  fun getFileReferenceCount(): Int {
    return references.size
  }

  fun getFileReference(index: Int): StorageReference {
    return references[index]
  }
}