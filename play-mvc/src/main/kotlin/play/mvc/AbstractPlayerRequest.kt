package play.mvc

abstract class AbstractPlayerRequest(@JvmField val playerId: Long, @JvmField val request: Request) {
  fun msgId() = request.msgId()
  val moduleId get() = request.header.moduleId
  val cmd get() = request.header.cmd

  override fun toString(): String {
    return "${javaClass.simpleName}(playerId=$playerId, request=$request)"
  }
}
