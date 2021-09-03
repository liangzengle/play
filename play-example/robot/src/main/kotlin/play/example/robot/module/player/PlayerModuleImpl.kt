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

  override fun createResp(player: RobotPlayer, data: Boolean, req: CreateRequestParams?) {
    loginReq(player)
  }

  override fun loginResp(player: RobotPlayer, data: PlayerDTO, req: Any?) {
    player.id = data.Id
    player.name = data.name
    println("$player logged in")
    pingReq(player, "hello")
  }

  override fun pingResp(player: RobotPlayer, data: String, req: PingRequestParams?) {
    logger.info("$player >> pong: ${req?.msg} $data")
    pingReq(player, "hello")
  }

  override fun gmResp(player: RobotPlayer, data: GmResult, req: GmRequestParams?) {
    println("gm指令使用成功: ${req?.cmd}")
  }

  override fun gmError(player: RobotPlayer, statusCode: Int, data: GmResult?, req: GmRequestParams?) {
    System.err.println("gm指令使用失败: ${req?.cmd} $statusCode")
  }

  override fun StringMessageResp(player: RobotPlayer, data: String, req: Any?) {
    TODO("Not yet implemented")
  }
}
