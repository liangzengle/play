package play.mvc

import kotlinx.serialization.Serializable
import play.util.collection.EmptyByteArray
import play.util.collection.EmptyIntArray
import play.util.collection.EmptyLongArray
import play.util.collection.EmptyStringArray

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

data class Response(
  @JvmField val header: Header,
  @JvmField val statusCode: Int,
  @JvmField val body: Any? = null
)

inline class MsgId(val value: Int) {
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

inline class Header(private val value: Long) {
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
}
