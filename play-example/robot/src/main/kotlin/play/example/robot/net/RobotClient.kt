package play.example.robot.net

import play.example.robot.module.player.RobotPlayer
import play.mvc.Request
import play.net.netty.NettyClient

class RobotClient(private val nettyClient: NettyClient) {

  private lateinit var player: RobotPlayer

  fun write(msg: Request) {
    nettyClient.write(msg)
  }

  fun connect() {
    nettyClient.connect()
  }

  fun setPlayer(player: RobotPlayer) {
    this.player = player
  }

  fun getPlayer(): RobotPlayer {
    return player
  }
}
