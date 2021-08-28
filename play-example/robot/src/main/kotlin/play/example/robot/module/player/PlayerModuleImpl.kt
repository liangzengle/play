package play.example.robot.module.player

import mu.KLogging
import org.springframework.stereotype.Component
import play.example.game.app.module.player.message.PlayerDTO
import play.example.game.container.gm.GmResult
import play.example.robot.module.PlayerModule

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerModuleImpl : PlayerModule() {
  companion object : KLogging()

  override fun createResp(player: RobotPlayer, statusCode: Int, data: Boolean, req: CreateRequestParams?) {
    if (statusCode == 0) {
      loginReq(player)
    }
  }

  override fun loginResp(player: RobotPlayer, statusCode: Int, data: PlayerDTO, req: Any?) {
    player.id = data.Id
    player.name = data.name
    println("$player logged in")
    pingReq(player, "hello")
  }

  override fun pingResp(player: RobotPlayer, statusCode: Int, data: String, req: PingRequestParams?) {
    logger.info("$player >> pong: ${req?.msg} $data")
    pingReq(player, "hello")
  }

  override fun gmResp(player: RobotPlayer, statusCode: Int, data: GmResult, req: GmRequestParams?) {
    if (statusCode != 0) {
      System.err.println("gm指令使用失败: ${req?.cmd} $statusCode")
    }
  }

  override fun StringMessageResp(player: RobotPlayer, statusCode: Int, data: String, req: Any?) {
    TODO("Not yet implemented")
  }
}
