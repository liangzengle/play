package play.example.game.app.module.friend

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.friend.message.FriendInfo
import play.mvc.*
import play.util.concurrent.PlayFuture
import play.util.control.ok

/**
 * 好友模块请求处理
 */
@Component
@Controller(ModuleId.Friend)
class FriendController(
  private val friendService: FriendService
) : AbstractController(ModuleId.Friend) {

  @Cmd(1)
  fun getInfo(playerId: Long) = friendService.getInfo(playerId).toRequestResult()

  @Cmd(2)
  fun getInfo2(playerId: Long) = friendService.getInfo2(playerId).toRequestResult()

  @Cmd(3)
  fun getInfo3(playerId: Long) = RequestResult.async {
    PlayFuture.successful(ok(FriendInfo()))
  }
}
