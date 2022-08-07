package play.mvc

import play.codec.MessageCodec
import play.util.EmptyByteArray
import play.util.EmptyIntArray
import play.util.EmptyLongArray

/**
 * @author LiangZengle
 */
class KProtobufRequestBodyBuilder : RequestBodyBuilder {

  private var b1: Boolean = false
  private var b2: Boolean = false
  private var b3: Boolean = false
  private var i1: Int = 0
  private var i2: Int = 0
  private var i3: Int = 0
  private var l1: Long = 0
  private var l2: Long = 0
  private var l3: Long = 0
  private var s1: String = ""
  private var s2: String = ""
  private var s3: String = ""
  private var ints: IntArray = EmptyIntArray
  private var longs: LongArray = EmptyLongArray
  private var bytes: ByteArray = EmptyByteArray

  private var boolIndex = 0
  private var intIndex = 0
  private var longIndex = 0
  private var stringIndex = 0

  override fun write(value: Boolean): RequestBodyBuilder {
    when (boolIndex) {
      0 -> b1 = value
      1 -> b2 = value
      2 -> b3 = value
      else -> throw IndexOutOfBoundsException(boolIndex)
    }
    boolIndex++
    return this
  }

  override fun write(value: Int): RequestBodyBuilder {
    when (intIndex) {
      0 -> i1 = value
      1 -> i2 = value
      2 -> i3 = value
      else -> throw IndexOutOfBoundsException(intIndex)
    }
    intIndex++
    return this
  }

  override fun write(value: Long): RequestBodyBuilder {
    when (longIndex) {
      0 -> l1 = value
      1 -> l2 = value
      2 -> l3 = value
      else -> throw IndexOutOfBoundsException(longIndex)
    }
    longIndex++
    return this
  }

  override fun write(value: String): RequestBodyBuilder {
    when (stringIndex) {
      0 -> s1 = value
      1 -> s2 = value
      2 -> s3 = value
      else -> throw IndexOutOfBoundsException(stringIndex)
    }
    stringIndex++
    return this
  }

  override fun write(array: IntArray): RequestBodyBuilder {
    require(this.ints.isEmpty()) { "ints is assigned to ${this.ints.contentToString()}" }
    this.ints = array
    return this
  }

  override fun writeIntList(intList: List<Int>): RequestBodyBuilder {
    require(this.ints.isEmpty()) { "ints is assigned to ${this.ints.contentToString()}" }
    this.ints = intList.toIntArray()
    return this
  }

  override fun write(array: LongArray): RequestBodyBuilder {
    require(this.longs.isEmpty()) { "longs is assigned to ${this.longs.contentToString()}" }
    this.longs = array
    return this
  }

  override fun writeLongList(longList: List<Long>): RequestBodyBuilder {
    require(this.longs.isEmpty()) { "longs is assigned to ${this.longs.contentToString()}" }
    this.longs = longList.toLongArray()
    return this
  }

  override fun write(array: ByteArray): RequestBodyBuilder {
    require(this.bytes.isEmpty()) { "bytes is assigned to ${this.bytes.contentToString()}" }
    this.bytes = array
    return this
  }

  override fun write(obj: Any): RequestBodyBuilder {
    write(MessageCodec.encode(obj))
    return this
  }

  override fun build(): RequestBody {
    return KProtobufRequestBody(
      b1,
      b2,
      b3,
      i1,
      i2,
      i3,
      l1,
      l2,
      l3,
      s1,
      s2,
      s3,
      ints,
      longs,
      bytes
    )
  }
}
