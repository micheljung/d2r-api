package ch.micheljung.d2rapi.blizzard.casc.info

class FieldDescriptor(encoded: String) {
  val name: String

  /**
   * The number of bytes required to represent the field in native form. A value of 0 means the field is variable
   * length.
   */
  val size: Int
  private val dataType: FieldDataType

  companion object {
    private const val NAME_TERMINATOR = '!'
    private const val DATA_TYPE_TERMINATOR = ':'
  }

  init {
    val nameEnd = encoded.indexOf(NAME_TERMINATOR)
    require(nameEnd != -1) { "Missing name terminator" }

    val dataTypeEnd = encoded.indexOf(DATA_TYPE_TERMINATOR, nameEnd + 1)
    require(dataTypeEnd != -1) { "Missing data type terminator" }

    name = encoded.substring(0, nameEnd)
    dataType = try {
      FieldDataType.valueOf(encoded.substring(nameEnd + 1, dataTypeEnd))
    } catch (e: IllegalArgumentException) {
      FieldDataType.UNSUPPORTED
    }
    size = encoded.substring(dataTypeEnd + 1).toInt()
  }
}