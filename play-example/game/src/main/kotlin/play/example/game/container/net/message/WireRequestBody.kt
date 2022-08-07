package play.example.game.container.net.message

import com.squareup.wire.Message
import play.codec.MessageCodec
import play.codec.wire.DelegatingMessage
import play.codec.wire.Wire
import play.example.net.message.RequestParams
import play.mvc.RequestBody

/**
 *
 *
 * @author LiangZengle
 */
class WireRequestBody(private val proto: RequestParams) : RequestBody, DelegatingMessage {

  private var booleanIndex = 0
  private var intIndex = 0
  private var longIndex = 0
  private var stringIndex = 0

  override fun getMessage(): Message<*, *> {
    return proto
  }

  override fun reset() {
    booleanIndex = 0
    intIndex = 0
    longIndex = 0
    stringIndex = 0
  }

  override fun readBoolean(): Boolean {
    val value = when (booleanIndex) {
      0 -> proto.b1
      1 -> proto.b2
      2 -> proto.b3
      else -> throw IndexOutOfBoundsException("booleanIndex: $booleanIndex")
    }
    booleanIndex++
    return value
  }

  override fun readInt(): Int {
    val value = when (intIndex) {
      0 -> proto.i1
      1 -> proto.i2
      2 -> proto.i3
      else -> throw IndexOutOfBoundsException("intIndex: $intIndex")
    }
    intIndex++
    return value
  }

  override fun readLong(): Long {
    val value = when (longIndex) {
      0 -> proto.l1
      1 -> proto.l2
      2 -> proto.l3
      else -> throw IndexOutOfBoundsException("longIndex: $longIndex")
    }
    longIndex++
    return value
  }

  override fun readString(): String {
    val value = when (stringIndex) {
      0 -> proto.s1
      1 -> proto.s2
      2 -> proto.s3
      else -> throw IndexOutOfBoundsException("stringIndex: $stringIndex")
    }
    stringIndex++
    return value
  }

  override fun getIntList(): List<Int> {
    return proto.ints
  }

  override fun getIntArray(): IntArray {
    return proto.ints.toIntArray()
  }

  override fun getLongList(): List<Long> {
    return proto.longs
  }

  override fun getLongArray(): LongArray {
    return proto.longs.toLongArray()
  }

  override fun getPayload(): ByteArray {
    return Wire.getInternalArray(proto.payload)
  }

  override fun <T : Any> decodePayloadAs(type: Class<T>): T {
    return MessageCodec.decode(proto.payload.asByteBuffer(), type)
  }

  override fun toString(): String {
    return proto.toString()
  }
}
