package ch.micheljung.d2rapi.blizzard.casc.storage

import ch.micheljung.d2rapi.blizzard.casc.Key
import ch.micheljung.d2rapi.blizzard.casc.nio.MalformedCascStructureException
import java.io.EOFException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Allows high level access to stored file data banks. These data banks can be
 * assembled using higher level logic into a continuous file.
 */
class BankStream(storageBuffer: ByteBuffer, encodingKey: Key?) {
  private val container: StorageContainer
  private val content: Array<BLTEContent>?
  private val streamBuffer: ByteBuffer
  private var bank = 0
  private var hasBanks = false

  /**
   * Get the length of the next bank in bytes.
   *
   * @return Length of bank in bytes.
   * @throws EOFException If there are no more banks in this stream.
   */
  @get:Throws(EOFException::class)
  val nextBankLength: Long
    get() {
      if (!hasNextBank()) {
        throw EOFException("No more banks to decode")
      }
      return if (content!!.isNotEmpty()) content[bank].decompressedSize else streamBuffer.remaining().toLong()
    }

  /**
   * Decode a bank from the stream. The bank buffer must be large enough to
   * receive the bank data as specified by getNextBankLength. A null buffer will
   * automatically allocate one large enough. The position of the bank buffer will
   * be advanced as appropriate, potentially allowing for many banks to be fetched
   * in sequence.
   *
   * @param bankBuffer Buffer to receive bank data.
   * @return If null then a new suitable buffer, otherwise bankBuffer.
   * @throws IOException  If something goes wrong during bank extraction.
   * @throws EOFException If there are no more banks in this stream.
   */
  fun getBank(bankBuffer: ByteBuffer?): ByteBuffer {
    var bankBuffer = bankBuffer
    if (!hasNextBank()) {
      throw EOFException("No more banks to decode")
    }
    if (content!!.isEmpty()) {
      // this logic is guessed and requires confirmation
      if (bankBuffer == null) {
        bankBuffer = ByteBuffer.allocate(streamBuffer.remaining())
      } else if (bankBuffer.remaining() < streamBuffer.remaining()) {
        throw MalformedCascStructureException("Bank buffer too small")
      }
      bankBuffer!!.put(streamBuffer)
      hasBanks = false
    } else {
      val blteEntry = content[bank]
      val encodedSize = blteEntry.compressedSize
      val decodedSize = blteEntry.decompressedSize
      when {
        streamBuffer.remaining() < encodedSize -> throw MalformedCascStructureException("Encoded data beyond end of file")
        bankBuffer == null -> {
          if (decodedSize > Int.MAX_VALUE) {
            throw MalformedCascStructureException("Bank too large for Java to manipulate")
          }
          bankBuffer = ByteBuffer.allocate(decodedSize.toInt())
        }
        bankBuffer.remaining() < decodedSize -> throw BufferOverflowException()
      }

      val encodedBuffer = streamBuffer.slice().limit(encodedSize.toInt()).slice()
      val decodedBuffer = bankBuffer!!.slice().limit(decodedSize.toInt()).slice()
      val encodingMode = encodedBuffer.get().toChar()

      when (encodingMode) {
        'N' -> {
          // uncompressed data
          if (encodedBuffer.remaining().toLong() != decodedSize) {
            throw MalformedCascStructureException("Not enough uncompressed bytes")
          }
          decodedBuffer.put(encodedBuffer)
        }
        'Z' -> {
          // zlib compressed data
          val zlib = Inflater()
          zlib.setInput(encodedBuffer)
          val resultSize: Int
          resultSize = try {
            zlib.inflate(decodedBuffer)
          } catch (e: DataFormatException) {
            throw MalformedCascStructureException("ZLIB inflate exception", e)
          }
          if (resultSize.toLong() != decodedSize) {
            throw MalformedCascStructureException("Not enough bytes generated: " + resultSize + "B")
          } else if (!zlib.finished()) {
            throw MalformedCascStructureException("Unfinished inflate operation")
          }
        }
        else -> throw UnsupportedEncodingException("Unsupported encoding mode: $encodingMode")
      }
      streamBuffer.position(streamBuffer.position() + encodedBuffer.position())
      bankBuffer.position(bankBuffer.position() + decodedBuffer.position())
      bank += 1
      if (bank == content.size) {
        hasBanks = false
      }
    }
    return bankBuffer
  }

  /**
   * Returns true while one or more banks are remaining to be streamed. Only valid
   * if hasBanks returns true.
   *
   * @return True if another bank can be decoded, otherwise false.
   */
  fun hasNextBank(): Boolean {
    return hasBanks
  }

  /**
   * Constructs a bank steam from the given buffer. An optional key can be used to
   * verify the right file is being processed. If a key is provided it is assumed
   * the remaining size of the buffer exactly matches the container size.
   *
   * @param storageBuffer Storage buffer, as specified by an index file.
   * @param encodingKey           File encoding key to check contents with, or null if no
   * such check is required.
   * @throws IOException If an exception occurs during decoding of the
   * storageBuffer.
   */
  init {
    var streamBuffer = storageBuffer.slice()
    container = StorageContainer(streamBuffer)
    if (encodingKey != null && container.key != encodingKey) {
      throw MalformedCascStructureException("Container encoding key mismatch")
    }
    val storageSize = container.size.toInt()
    val storageSizeDiff = Integer.compare(streamBuffer.capacity(), storageSize)
    if (storageSizeDiff < 0) {
      throw MalformedCascStructureException("Container buffer smaller than container")
    }
    if (encodingKey != null && storageSizeDiff != 0) {
      throw MalformedCascStructureException("Container buffer size mismatch")
    }
    if (storageSizeDiff > 0) {
      // resize buffer to match file
      val streamPos = streamBuffer.position()
      streamBuffer.limit(storageSize)
      streamBuffer.position(0)
      streamBuffer = streamBuffer.slice()
      streamBuffer.position(streamPos)
    }
    if (streamBuffer.hasRemaining()) {
      content = BLTEContent.decodeContent(streamBuffer)
      hasBanks = true
    } else {
      content = null
      hasBanks = false
    }
    this.streamBuffer = streamBuffer
    storageBuffer.position(storageBuffer.position() + streamBuffer.capacity())
  }
}