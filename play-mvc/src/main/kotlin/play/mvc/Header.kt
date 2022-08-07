package play.mvc

import play.util.primitive.toLong

@JvmInline
value class Header(private val value: Long) {
  constructor(msgId: MsgId, sequenceNo: Int) : this(toLong(msgId.toInt(), sequenceNo))
  constructor(msgId: MsgId) : this(msgId, 0)

  val msgId: MsgId get() = MsgId((value shr 32 and 0x7ffffff).toInt())

  val requestId: Int get() = value.toInt()

  val moduleId: Short get() = msgId.moduleId

  val cmd: Byte get() = msgId.cmd

  override fun toString(): String {
    return "Header($msgId, $requestId)"
  }
}
