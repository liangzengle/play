package play.mvc

class Push<T>(val msgId: MsgId) {
  constructor(moduleId: Short, cmd: Byte) : this(MsgId(moduleId, cmd))

  @JvmName("of")
  operator fun invoke(message: Any?): Response {
    return Response(Header(msgId), 0, message)
  }
}
