package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.Key

class IndexEntry(
  key: ByteArray,
  /** Logical offset of storage container. */
  val dataOffset: Long,
  /** Size of storage container. */
  val fileSize: Long
) {
  /** Index encoding key. */
  val key: Key = Key(key)
}