package play.example.robot.module.guild

import org.springframework.stereotype.Component
import play.example.module.guild.message.GuildProto
import play.example.robot.module.GuildModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class GuildModuleImpl : GuildModule() {
  override fun joinResp(player: RobotPlayer, data: GuildProto, req: JoinRequestParams?) {
    TODO("Not yet implemented")
  }

  override fun createResp(player: RobotPlayer, data: GuildProto, req: CreateRequestParams?) {
    TODO("Not yet implemented")
  }

}
