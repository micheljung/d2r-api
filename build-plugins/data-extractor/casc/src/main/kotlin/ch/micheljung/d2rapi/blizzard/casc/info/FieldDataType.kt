package ch.micheljung.d2rapi.blizzard.casc.info

enum class FieldDataType {
  /** Field contains textual data. Size is ignored. */
  STRING,

  /** Field is a decimal number. Size determines the number of bytes used to represent it. */
  DEC,

  /**
   * Hexadecimal string. Size is the number of bytes used to represent it with every 2 characters representing 1 byte.
   */
  HEX,

  /** Field type is currently not supported. */
  UNSUPPORTED
}