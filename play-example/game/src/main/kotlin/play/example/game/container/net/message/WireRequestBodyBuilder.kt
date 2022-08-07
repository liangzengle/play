package play.example.game.container.net.message

import okio.ByteString
import play.codec.MessageCodec
import play.codec.wire.WireMessageCodec
import play.example.net.message.RequestParams
import play.mvc.RequestBody
import play.mvc.RequestBodyBuilder

/**
 *
 *
 * @author LiangZengle
 */
class WireRequestBodyBuilder : RequestBodyBuilder {

  private var b1 = false
  private var b2 = false
  private var b3 = false
  private var i1 = 0
  private var i2 = 0
  private var i3 = 0
  private var l1 = 0L
  private var l2 = 0L
  private var l3 = 0L
  private var s1 = ""
  private var s2 = ""
  private var s3 = ""
  private var ints = emptyList<Int>()
  private var longs = emptyList<Long>()
  private var payload = ByteString.EMPTY


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
    ints = array.asList()
    return this
  }

  override fun write(array: LongArray): RequestBodyBuilder {
    longs = array.asList()
    return this
  }

  override fun writeIntList(intList: List<Int>): RequestBodyBuilder {
    this.ints = intList
    return this
  }

  override fun writeLongList(longList: List<Long>): RequestBodyBuilder {
    this.longs = longList
    return this
  }

  override fun write(array: ByteArray): RequestBodyBuilder {
    payload = ByteString.of(*array)
    return this
  }

  override fun write(obj: Any): RequestBodyBuilder {
    val codec = MessageCodec.Default
    payload = if (codec is WireMessageCodec) {
      codec.encodeToByteString(obj)
    } else {
      ByteString.of(*codec.encode(obj))
    }
    return this
  }

  override fun build(): RequestBody {
    return WireRequestBody(
      RequestParams(b1, b2, b3, i1, i2, i3, l1, l2, l3, s1, s2, s3, ints, longs, payload)
    )
  }
}
