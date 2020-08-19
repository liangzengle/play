package play.mvc

import play.util.collection.EmptyByteArray

interface Message {
  fun toByteArray(): ByteArray
}

class ByteArrayMessage(private val array: ByteArray) : Message {
  override fun toByteArray(): ByteArray = array

  companion object {
    val Empty = ByteArrayMessage(EmptyByteArray)
  }
}

data class Request(@JvmField val header: Header, @JvmField val body: RequestBody)

data class Response(
  @JvmField val header: Header,
  @JvmField val statusCode: Int,
  @JvmField val body: Message = ByteArrayMessage.Empty
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

interface RequestBody {

  fun reset()

  fun readBoolean(): Boolean

  fun readInt(): Int

  fun readLong(): Long

  fun readString(): String

  fun readIntList(): List<Int>

  fun readLongList(): List<Long>

  fun readByteArray(): ByteArray

  fun toByteArray(): ByteArray
}
