package play.example.robot.module.player

import mu.KLogging
import org.springframework.stereotype.Component
import play.example.player.message.PlayerProto
import play.example.reward.message.RewardResultSetProto
import play.example.robot.module.CommandModule
import play.example.robot.module.PlayerModule
import java.util.concurrent.TimeUnit

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

  override fun changeNameResp(player: RobotPlayer, data: String, req: ChangeNameRequestParams?) {
    TODO("Not yet implemented")
  }

  override fun loginResp(player: RobotPlayer, data: PlayerProto, req: Any?) {
    player.id = data.id
    player.name = data.name
    println("$player logged in")
    commandModule.listReq(player)

    player.eventLoop.scheduleWithFixedDelay({ pingReq(player, "hello") }, 0, 10, TimeUnit.SECONDS)
  }

  override fun pingResp(player: RobotPlayer, data: String, req: PingRequestParams?) {
    logger.info("$player >> pong: ${req?.msg} $data")
  }

  override fun stringMessageResp(player: RobotPlayer, data: String, req: Any?) {
    TODO("Not yet implemented")
  }

  override fun rewardOrCostResp(player: RobotPlayer, data: RewardResultSetProto, req: Any?) {
    TODO("Not yet implemented")
  }
}
