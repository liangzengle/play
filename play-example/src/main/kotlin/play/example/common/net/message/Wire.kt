package play.example.common.net.message

import play.example.request.RequestProto
import play.mvc.*
import play.util.collection.EmptyByteArray
import play.util.control.Result2
import play.util.control.map

class WireMessage(val msg: com.squareup.wire.Message<*, *>) : Message {
  override fun toByteArray(): ByteArray = msg.encode()
}

@Suppress("UNCHECKED_CAST")
@JvmName("ok")
@OverloadResolutionByLambdaReturnType
inline fun <T : com.squareup.wire.Message<*, *>> RequestResult.Companion.ok(f: () -> T): RequestResult<T> {
  return RequestResult.Ok(WireMessage(f())) as RequestResult<T>
}

@Suppress("UNCHECKED_CAST")
@JvmName("of")
@OverloadResolutionByLambdaReturnType
inline fun <T> RequestResult.Companion.of(f: () -> Result2<com.squareup.wire.Message<*, *>>): RequestResult<T> {
  return RequestResult(f().map { WireMessage(it) }) as RequestResult<T>
}

operator fun Push<*>.invoke() = Response(Header(msgId, 0), 0)

operator fun <T : com.squareup.wire.Message<*, *>> Push<T>.invoke(value: T) =
  Response(Header(msgId, 0), 0, WireMessage(value))


class WireRequestBody(private val proto: RequestProto) : RequestBody {
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
      0 -> proto.b1!!
      1 -> proto.b2!!
      2 -> proto.b3!!
      else -> throw IndexOutOfBoundsException("booleanIndex=$booleanIndex")
    }
    booleanIndex += 1
    return value
  }

  override fun readInt(): Int {
    val value = when (intIndex) {
      0 -> proto.i1!!
      1 -> proto.i2!!
      2 -> proto.i3!!
      else -> throw IndexOutOfBoundsException("booleanIndex=$intIndex")
    }
    intIndex += 1
    return value
  }

  override fun readLong(): Long {
    val value = when (longIndex) {
      0 -> proto.l1!!
      1 -> proto.l2!!
      2 -> proto.l3!!
      else -> throw IndexOutOfBoundsException("booleanIndex=$longIndex")
    }
    longIndex += 1
    return value
  }

  override fun readString(): String {
    val value = when (stringIndex) {
      0 -> proto.s1!!
      1 -> proto.s2!!
      2 -> proto.s3!!
      else -> throw IndexOutOfBoundsException("stringIndex=$stringIndex")
    }
    stringIndex += 1
    return value
  }

  override fun readIntList(): List<Int> {
    return proto.intList
  }

  override fun readLongList(): List<Long> {
    return proto.longList
  }

  override fun readByteArray(): ByteArray {
    return proto.byteList?.toByteArray() ?: EmptyByteArray
  }

  override fun toByteArray(): ByteArray {
    return proto.encode()
  }
}
