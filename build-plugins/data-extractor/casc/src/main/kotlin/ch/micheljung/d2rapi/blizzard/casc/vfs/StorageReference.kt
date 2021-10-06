package ch.micheljung.d2rapi.blizzard.casc.vfs

import ch.micheljung.d2rapi.blizzard.casc.Key


/**
 * A reference to part of a file in CASC storage.
 */
class StorageReference(
    /** Logical offset of this chunk. The exact mechanic of this is speculative as the only observed value is 0. */
    val offset: Long,

    /**
     * Logical size of this chunk. The exact mechanic of this is speculative as the only observed value is identical
     * to actual size.
     */
    val size: Long,

    /** Encoding key of chunk. */
    val encodingKey: Key,

    physicalSize: Int,
    /**
     * Member that purpose is currently not know. Known values are 0x00 and 0x0A.
     *
     * This is a temporary method used to help diagnose the purpose of the member.
     * It should not be used in production code.
     *
     */
    private val unknownMember1: Byte, actualSize: Int
) {

    /** Physical size of stored data banks in bytes. This is the size occupied by the data in the CASC local storage. */
    private val physicalSize: Long = physicalSize.toLong()

    /**
     * Logical size of data banks.
     *
     * Get the total logical size of the associated data banks. This is the size
     * occupied by the data once expanded from local storage.
     *
     * It should match chunk size as otherwise there would unaccounted bytes.
     */
    val actualSize: Long = actualSize.toLong()

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("FileReference{encodingKey=")
        builder.append(encodingKey)
        builder.append(", offset=")
        builder.append(offset)
        builder.append(", size=")
        builder.append(size)
        builder.append(", physicalSize=")
        builder.append(physicalSize)
        builder.append(", unknownMember1=")
        builder.append(unknownMember1.toInt())
        builder.append(", actualSize=")
        builder.append(actualSize)
        builder.append("}")
        return builder.toString()
    }
}