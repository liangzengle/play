package play.example.robot.net

import play.example.robot.module.player.RobotPlayer
import play.mvc.Request
import play.net.netty.NettyClient
import play.util.concurrent.PlayFuture

class RobotClient(private val nettyClient: NettyClient) {

  private lateinit var player: RobotPlayer

  fun write(msg: Request) {
    nettyClient.write(msg)
  }

  fun connect(): PlayFuture<Unit> {
    return nettyClient.connect()
  }

  fun setPlayer(player: RobotPlayer) {
    this.player = player
  }

  fun getPlayer(): RobotPlayer {
    return player
  }
}
