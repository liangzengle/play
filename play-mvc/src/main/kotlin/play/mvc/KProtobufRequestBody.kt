package play.mvc

import kotlinx.serialization.Serializable
import play.codec.MessageCodec
import play.util.EmptyByteArray
import play.util.EmptyIntArray
import play.util.EmptyLongArray

/**
 *
 *
 * @author LiangZengle
 */
@Serializable
class KProtobufRequestBody(
  val b1: Boolean = false,
  val b2: Boolean = false,
  val b3: Boolean = false,
  val i1: Int = 0,
  val i2: Int = 0,
  val i3: Int = 0,
  val l1: Long = 0,
  val l2: Long = 0,
  val l3: Long = 0,
  val s1: String = "",
  val s2: String = "",
  val s3: String = "",
  val ints: IntArray = EmptyIntArray,
  val longs: LongArray = EmptyLongArray,
  private val payload: ByteArray = EmptyByteArray
) : RequestBody {
  companion object {
    @JvmStatic
    val Empty = KProtobufRequestBody()
  }

  private var booleanIndex = 0
  private var intIndex = 0
  private var longIndex = 0
  private var stringIndex = 0

  override fun reset() {
    booleanIndex = 0
    intIndex = 0
    longIndex = 0
    stringIndex = 0
  }

  override fun readBoolean(): Boolean {
    val value = when (booleanIndex) {
      0 -> b1
      1 -> b2
      2 -> b3
      else -> throw IndexOutOfBoundsException("booleanIndex: $booleanIndex")
    }
    booleanIndex++
    return value
  }

  override fun readInt(): Int {
    val value = when (intIndex) {
      0 -> i1
      1 -> i2
      2 -> i3
      else -> throw IndexOutOfBoundsException("intIndex: $intIndex")
    }
    intIndex++
    return value
  }

  override fun readLong(): Long {
    val value = when (longIndex) {
      0 -> l1
      1 -> l2
      2 -> l3
      else -> throw IndexOutOfBoundsException("longIndex: $longIndex")
    }
    longIndex++
    return value
  }

  override fun readString(): String {
    val value = when (stringIndex) {
      0 -> s1
      1 -> s2
      2 -> s3
      else -> throw IndexOutOfBoundsException("stringIndex: $stringIndex")
    }
    stringIndex++
    return value
  }

  override fun getIntList(): List<Int> {
    return ints.asList()
  }

  override fun getIntArray(): IntArray {
    return ints
  }

  override fun getLongList(): List<Long> {
    return longs.asList()
  }

  override fun getPayload(): ByteArray {
    return payload
  }

  override fun getLongArray(): LongArray {
    return longs
  }

  override fun <T : Any> decodePayloadAs(type: Class<T>): T {
    return MessageCodec.decode(payload, type)
  }

  override fun toString(): String {
    val b = StringBuilder(64)
    b.append("RequestBody(")
    if (b1) {
      b.append("b1=").append(b1)
    }
    if (b2) {
      b.append("b2=").append(b2)
    }
    if (b3) {
      b.append("b3=").append(b3)
    }
    if (i1 != 0) {
      b.append("i1=").append(i1)
    }
    if (i2 != 0) {
      b.append("i2=").append(i2)
    }
    if (i3 != 0) {
      b.append("i3=").append(i3)
    }
    if (l1 != 0.toLong()) {
      b.append("l1=").append(l1)
    }
    if (l2 != 0.toLong()) {
      b.append("l2=").append(l2)
    }
    if (l3 != 0.toLong()) {
      b.append("l3=").append(l1)
    }
    if (s1 != "") {
      b.append("s1=").append(s1)
    }
    if (s2 != "") {
      b.append("s2=").append(s2)
    }
    if (s3 != "") {
      b.append("s3=").append(s3)
    }
    if (ints.isNotEmpty()) {
      b.append("ints=").append(ints.contentToString())
    }
    if (longs.isNotEmpty()) {
      b.append("longs=").append(longs.contentToString())
    }
    if (payload.isNotEmpty()) {
      b.append("payload=").append(payload.contentToString())
    }
    b.append(')')
    return b.toString()
  }
}
