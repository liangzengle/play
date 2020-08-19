package play.mvc

class Push<T>(val msgId: MsgId) {
  constructor(moduleId: Short, cmd: Byte) : this(MsgId(moduleId, cmd))
}
