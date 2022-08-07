package play.example.robot.module.friend

import org.springframework.stereotype.Component
import play.example.module.friend.message.FriendInfo
import play.example.robot.module.FriendModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class FriendModuleImpl : FriendModule() {
  override fun getInfo2Resp(player: RobotPlayer, data: FriendInfo, req: GetInfo2RequestParams?) {
    TODO("Not yet implemented")
  }

  override fun getInfo3Resp(player: RobotPlayer, data: FriendInfo, req: GetInfo3RequestParams?) {
    TODO("Not yet implemented")
  }

  override fun getInfoResp(player: RobotPlayer, data: FriendInfo, req: GetInfoRequestParams?) {
    TODO("Not yet implemented")
  }
}
