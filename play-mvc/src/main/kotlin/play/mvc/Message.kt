package play.mvc

import kotlinx.serialization.Serializable
import play.util.*

data class Request(@JvmField val header: Header, @JvmField val body: RequestBody) {
  fun msgId() = header.msgId.toInt()
}

abstract class AbstractPlayerRequest(@JvmField val playerId: Long, @JvmField val request: Request) {
  fun msgId() = request.msgId()
  val moduleId get() = request.header.moduleId
  val cmd get() = request.header.cmd

  override fun toString(): String {
    return "${javaClass.simpleName}(playerId=$playerId, request=$request)"
  }
}

class PlayerRequest(playerId: Long, request: Request) : AbstractPlayerRequest(playerId, request)

class Response(
  @JvmField val header: Header,
  @JvmField val statusCode: Int,
  @JvmField val body: ByteArray = EmptyByteArray
) {
  constructor(header: Header, statusCode: Int, data: Any?) : this(header, statusCode, MessageCodec.encode(data))

  override fun toString(): String {
    return "Response(header=$header, statusCode=$statusCode, body=${body.contentToString()})"
  }
}

@JvmInline
value class MsgId(val value: Int) {
  constructor(moduleId: Short, cmd: Byte) : this(moduleId.toInt() shl 8 or cmd.toInt())

  val moduleId: Short get() = (value shr 8 and 0x7fff).toShort()

  val cmd: Byte get() = (value and 0xff).toByte()

  fun toInt(): Int = value

  fun isEqual(moduleId: Short, cmd: Byte): Boolean {
    return moduleId == this.moduleId && cmd == this.cmd
  }

  override fun toString(): String {
    return "MsgId($moduleId, $cmd)"
  }
}

@JvmInline
value class Header(private val value: Long) {
  constructor(msgId: MsgId, sequenceNo: Int) : this(msgId.value.toLong() shl 32 or sequenceNo.toLong())
  constructor(msgId: MsgId) : this(msgId, 0)

  val msgId: MsgId get() = MsgId((value shr 32 and 0x7ffffff).toInt())

  val sequenceNo: Int get() = value.toInt()

  val moduleId: Short get() = msgId.moduleId

  val cmd: Byte get() = msgId.cmd

  override fun toString(): String {
    return "Header($msgId, $sequenceNo)"
  }
}

@Serializable
class RequestBody(
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
  val strings: Array<String> = EmptyStringArray,
  val bytes: ByteArray = EmptyByteArray
) {
  companion object {
    @JvmStatic
    val Empty = RequestBody()
  }

  private var booleanIndex = 0
  private var intIndex = 0
  private var longIndex = 0
  private var stringIndex = 0

  fun reset() {
    booleanIndex = 0
    intIndex = 0
    longIndex = 0
    stringIndex = 0
  }

  fun readBoolean(): Boolean {
    val value = when (booleanIndex) {
      0 -> b1
      1 -> b2
      2 -> b3
      else -> throw IndexOutOfBoundsException("booleanIndex: $booleanIndex")
    }
    booleanIndex++
    return value
  }

  fun readInt(): Int {
    val value = when (intIndex) {
      0 -> i1
      1 -> i2
      2 -> i3
      else -> throw IndexOutOfBoundsException("intIndex: $intIndex")
    }
    intIndex++
    return value
  }

  fun readLong(): Long {
    val value = when (longIndex) {
      0 -> l1
      1 -> l2
      2 -> l3
      else -> throw IndexOutOfBoundsException("longIndex: $longIndex")
    }
    longIndex++
    return value
  }

  fun readString(): String {
    val value = when (stringIndex) {
      0 -> s1
      1 -> s2
      2 -> s3
      else -> throw IndexOutOfBoundsException("stringIndex: $stringIndex")
    }
    stringIndex++
    return value
  }

  fun getIntList(): List<Int> {
    return ints.asList()
  }

  fun getIntArray(): IntArray {
    return ints
  }

  fun getLongList(): List<Long> {
    return longs.asList()
  }

  fun getLongArray(): LongArray {
    return longs
  }

  fun getStringList(): List<String> {
    return strings.asList()
  }

  fun getStringArray(): Array<String> {
    return strings
  }

  fun getByteList(): List<Byte> {
    return bytes.asList()
  }

  fun getByteArray(): ByteArray {
    return bytes
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
    if (strings.isNotEmpty()) {
      b.append("strings=").append(strings.contentToString())
    }
    if (bytes.isNotEmpty()) {
      b.append("bytes=").append(bytes.contentToString())
    }
    b.append(')')
    return b.toString()
  }
}

class RequestBodyBuilder {
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
  private var strings: Array<String> = EmptyStringArray
  private var bytes: ByteArray = EmptyByteArray

  private var boolIndex = 0
  private var intIndex = 0
  private var longIndex = 0
  private var stringIndex = 0

  fun write(value: Boolean): RequestBodyBuilder {
    when (boolIndex) {
      0 -> b1 = value
      1 -> b2 = value
      2 -> b3 = value
      else -> throw IndexOutOfBoundsException(boolIndex)
    }
    boolIndex++
    return this
  }

  fun write(value: Int): RequestBodyBuilder {
    when (intIndex) {
      0 -> i1 = value
      1 -> i2 = value
      2 -> i3 = value
      else -> throw IndexOutOfBoundsException(boolIndex)
    }
    intIndex++
    return this
  }

  fun write(value: Long): RequestBodyBuilder {
    when (longIndex) {
      0 -> l1 = value
      1 -> l2 = value
      2 -> l3 = value
      else -> throw IndexOutOfBoundsException(boolIndex)
    }
    longIndex++
    return this
  }

  fun write(value: String): RequestBodyBuilder {
    when (stringIndex) {
      0 -> s1 = value
      1 -> s2 = value
      2 -> s3 = value
      else -> throw IndexOutOfBoundsException(boolIndex)
    }
    stringIndex++
    return this
  }

  fun write(array: IntArray): RequestBodyBuilder {
    this.ints = array
    return this
  }

  fun write(array: LongArray): RequestBodyBuilder {
    this.longs = array
    return this
  }

  fun write(array: Array<String>): RequestBodyBuilder {
    this.strings = array
    return this
  }

  fun write(array: ByteArray): RequestBodyBuilder {
    this.bytes = array
    return this
  }

  fun write(value: Any): RequestBodyBuilder {
    when (value) {
      is Boolean -> write(value)
      is Int -> write(value)
      is Long -> write(value)
      is String -> write(value)
      is IntArray -> write(value)
      is LongArray -> write(value)
      is ByteArray -> write(value)
      (value is Array<*> && value.isArrayOf<String>()) -> write(value.unsafeCast<Array<String>>())
      else -> throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")
    }
    return this
  }

  fun build(): RequestBody {
    return RequestBody(
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
      strings,
      bytes
    )
  }
}
