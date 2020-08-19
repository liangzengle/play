package play.mvc

import play.util.exception.NoStackTraceException

class NoSuchActionException(msgId: MsgId) : NoStackTraceException(msgId.toString()) {
  constructor(moduleId: Int, cmd: Int) : this(MsgId(moduleId.toShort(), cmd.toByte()))
}
