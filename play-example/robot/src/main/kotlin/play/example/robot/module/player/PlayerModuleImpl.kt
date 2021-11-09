package play.example.robot.module.player

import mu.KLogging
import org.springframework.stereotype.Component
import play.example.game.app.module.player.message.PlayerDTO
import play.example.robot.module.CommandModule
import play.example.robot.module.PlayerModule

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerModuleImpl(private val commandModule: CommandModule) : PlayerModule() {
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
//    pingReq(player, "hello")

    commandModule.listReq(player)
  }

  override fun StringMessageResp(player: RobotPlayer, data: String, req: Any?) {
    TODO("Not yet implemented")
  }
}
