package play.example.robot.module.guild

import org.springframework.stereotype.Component
import play.example.game.app.module.guild.message.GuildInfo
import play.example.robot.module.EquipModule
import play.example.robot.module.GuildModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class GuildModuleImpl : GuildModule(){
  override fun joinResp(player: RobotPlayer, statusCode: Int, data: GuildInfo, req: JoinRequestParams?) {
    TODO("Not yet implemented")
  }

  override fun createResp(player: RobotPlayer, statusCode: Int, data: GuildInfo, req: CreateRequestParams?) {
    TODO("Not yet implemented")
  }

}
