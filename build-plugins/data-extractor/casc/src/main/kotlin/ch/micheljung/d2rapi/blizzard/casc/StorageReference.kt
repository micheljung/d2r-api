package ch.micheljung.d2rapi.blizzard.casc

/** A reference to a file extracted from a configuration file.  */
class StorageReference(name: String, configuration: Map<String, String>) {

  /**
   * Size of file content in CASC storage.
   *
   * @return Approximate byte usage of file in CASC storage.
   */
  val storedSize: Long

  /** File size in bytes of the file. */
  val size: Long

  /**
   * Encoding key used to lookup the file from CASC storage.
   *
   * @return Encoding key.
   */
  val encodingKey: Key

  /**
   * Content key?
   *
   * @return Content key.
   */
  val contentKey: Key

  companion object {
    /** Suffix for sizes mapping entry in configuration files.  */
    private const val SIZES_SUFFIX = "-size"
  }

  /**
   * Decodes a storage reference from a configuration file.
   *
   * @param name          Name of reference.
   * @param configuration Map of configuration file content.
   */
  init {
    val keys = configuration[name] ?: throw IllegalArgumentException("'$name' does not exist in configuration")
    val sizes = configuration[name + SIZES_SUFFIX]
      ?: throw IllegalArgumentException("Size missing in configuration")
    val keyStrings = keys.split(" ").toTypedArray()
    contentKey = Key(keyStrings[0])
    encodingKey = Key(keyStrings[1])
    val sizeStrings = sizes.split(" ").toTypedArray()
    size = sizeStrings[0].toLong()
    storedSize = sizeStrings[1].toLong()
  }
}