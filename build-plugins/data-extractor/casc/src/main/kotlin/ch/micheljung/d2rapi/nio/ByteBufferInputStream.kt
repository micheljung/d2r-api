package ch.micheljung.d2rapi.nio

import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Simple InputStream wrapper for ByteBuffer.
 *
 *
 * This class is not thread safe.
 *
 *
 * https://stackoverflow.com/questions/4332264/wrapping-a-bytebuffer-with-an-inputstream
 */
class ByteBufferInputStream(var buf: ByteBuffer) : InputStream() {
  override fun read(): Int {
    return if (!buf.hasRemaining()) {
      -1
    } else buf.get().toInt() and 0xFF
  }

  override fun read(bytes: ByteArray, off: Int, len: Int): Int {
    var len = len
    if (!buf.hasRemaining()) {
      return -1
    }
    len = Math.min(len, buf.remaining())
    buf[bytes, off, len]
    return len
  }
}