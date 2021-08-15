package play.example.robot.module.player

import io.netty.channel.EventLoop
import play.example.robot.net.RequestParams
import play.example.robot.net.Requester
import play.example.robot.net.RobotClient

/**
 *
 * @author LiangZengle
 */
class RobotPlayer(val account: String, client: RobotClient, private val eventLoop: EventLoop) {
  var id: Long = 0

  var name: String = ""

  private val requester = Requester(client)

  init {
    client.setPlayer(this)
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
