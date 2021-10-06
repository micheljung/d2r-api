package ch.micheljung.d2rapi.blizzard.casc

import ch.micheljung.d2rapi.lang.Hex
import java.util.*
import kotlin.math.min

/**
 * Class representing a CASC related key such as an encoding key.
 *
 *
 * When testing equality and comparing the length of the shortest key is used.
 */
class Key : Comparable<Key> {
  /**
   * Key array.
   */
  val key: ByteArray

  /**
   * Wraps a byte array into a key. The array is used directly so must not be modified.
   *
   * @param key Key array.
   */
  constructor(key: ByteArray) {
    this.key = key
  }

  /**
   * Constructs a key from a hexadecimal key string. Bytes are order in the order
   * they appear in the string, which can be considered big endian.
   *
   * @param key hexadecimal key form of key.
   */
  constructor(key: CharSequence) {
    this.key = Hex.decodeHex(key)
  }

  override fun compareTo(other: Key): Int {
    val commonLength = min(key.size, other.key.size)
    return Arrays.compareUnsigned(key, 0, commonLength, other.key, 0, commonLength)
  }

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is Key) {
      return false
    }
    val commonLength = min(key.size, other.key.size)
    return Arrays.equals(key, 0, commonLength, other.key, 0, commonLength)
  }

  override fun hashCode(): Int {
    throw UnsupportedOperationException("Key hash code not safe to use due to variable sizes between systems")
  }

  override fun toString(): String {
    val builder = StringBuilder(key.size + 1)
    Hex.stringBufferAppendHex(builder, key)
    return builder.toString()
  }
}