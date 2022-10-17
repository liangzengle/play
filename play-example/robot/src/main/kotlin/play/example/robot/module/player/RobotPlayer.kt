package play.example.robot.module.player

import io.netty.channel.EventLoop
import io.netty.util.AttributeKey
import play.example.robot.net.RequestParams
import play.example.robot.net.Requester
import play.example.robot.net.RobotClient

/**
 *
 * @author LiangZengle
 */
class RobotPlayer(val account: String, val eventLoop: EventLoop) {
  companion object {
    val AttrKey: AttributeKey<RobotPlayer> = AttributeKey.valueOf("RobotPlayer")
  }

  var id: Long = 0

  var name: String = ""

  var serverId: Int = 1

  private lateinit var client: RobotClient

  private lateinit var requester: Requester

  fun setClient(client: RobotClient) {
    this.client = client
    this.requester = Requester(client)
  }

  fun getRequestParams(requestId: Int): RequestParams? {
    return requester.getRequestParams(requestId)
  }

  fun send(msgId: Int, params: RequestParams?) {
    requester.send(msgId, params)
  }

  fun execute(task: () -> Unit) {
    if (eventLoop.inEventLoop()) {
      task()
    } else {
      eventLoop.execute(task)
    }
  }

  override fun toString(): String {
    return "Player($id, $name)"
  }
}
