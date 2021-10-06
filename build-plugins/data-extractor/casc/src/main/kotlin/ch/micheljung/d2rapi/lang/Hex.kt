package ch.micheljung.d2rapi.lang

import java.util.*


object Hex {
  private val HEX_DIGITS = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  )
  private const val RADIX_SIZE = 4
  private val RADIX = HEX_DIGITS.size
  private const val NO_VALUE: Byte = -1
  private const val DECIMAL_CHARACTERS = 10
  private val CHAR_VALUES = ByteArray(1 shl java.lang.Byte.SIZE)

  private const val NIBBLE_MASK = 15
  private fun decodeNibble(codePoint: Int): Byte {
    return if (codePoint > CHAR_VALUES.size) {
      NO_VALUE
    } else {
      CHAR_VALUES[codePoint]
    }
  }

  fun decodeHex(hex: CharSequence): ByteArray {
    val nibbleCount = hex.length
    var valueNibbleShift = (nibbleCount - 1) % (java.lang.Byte.SIZE / RADIX_SIZE) * RADIX_SIZE
    val values = ByteArray(nibbleCount + 1 shr 1)
    var valueIndex = 0
    var value = 0
    var nibbleIndex = 0
    while (nibbleIndex < nibbleCount) {
      val codePoint = hex[nibbleIndex].toInt()
      val nibble: Byte = decodeNibble(codePoint)
      if (nibble == NO_VALUE) {
        throw NumberFormatException("Non-hex character: $codePoint")
      }
      value = value or (nibble.toInt() shl valueNibbleShift)
      if (valueNibbleShift == 0) {
        valueNibbleShift = java.lang.Byte.SIZE
        values[valueIndex++] = value.toByte()
        value = 0
      }
      valueNibbleShift -= RADIX_SIZE
      nibbleIndex += 1
    }
    return values
  }

  private fun stringBufferAppendHex(builder: StringBuilder, hex: Byte) {
    builder.append(HEX_DIGITS[hex.toInt() shr 4 and NIBBLE_MASK])
    builder.append(HEX_DIGITS[(hex.toInt() and NIBBLE_MASK)])
  }

  fun stringBufferAppendHex(builder: StringBuilder, hex: ByteArray) {
    var i = 0
    while (i < hex.size) {
      stringBufferAppendHex(builder, hex[i])
      i += 1
    }
  }

  init {
    Arrays.fill(CHAR_VALUES, NO_VALUE)
    var value = 0
    while (value < DECIMAL_CHARACTERS) {
      CHAR_VALUES['0'.toInt() + value] =
        value.toByte()
      value += 1
    }
    while (value < RADIX) {
      CHAR_VALUES['a'.toInt() - DECIMAL_CHARACTERS + value] = value.toByte()
      CHAR_VALUES['A'.toInt() - DECIMAL_CHARACTERS + value] = value.toByte()
      value += 1
    }
  }
}