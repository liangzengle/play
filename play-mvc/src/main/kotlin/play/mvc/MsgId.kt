package play.mvc

@JvmInline
value class MsgId(val value: Int) {
  companion object {
    @JvmStatic
    fun toInt(moduleId: Short, cmd: Byte): Int = moduleId.toInt() shl 8 or cmd.toInt()

    @JvmStatic
    fun getModuleId(msgId: Int): Short = (msgId shr 8 and 0x7fff).toShort()

    @JvmStatic
    fun getCmd(msgId: Int): Byte = (msgId and 0xff).toByte()
  }

  constructor(moduleId: Short, cmd: Byte) : this(toInt(moduleId, cmd))

  val moduleId: Short get() = getModuleId(value)

  val cmd: Byte get() = getCmd(value)

  fun toInt(): Int = value

  fun isEqual(moduleId: Short, cmd: Byte): Boolean {
    return moduleId == this.moduleId && cmd == this.cmd
  }

  override fun toString(): String {
    return "MsgId($moduleId, $cmd)"
  }
}
