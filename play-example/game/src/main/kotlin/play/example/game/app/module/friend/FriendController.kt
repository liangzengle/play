package play.example.game.app.module.friend

import play.example.game.app.module.ModuleId
import play.example.game.app.module.friend.message.FriendInfo
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.concurrent.PlayFuture
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 好友模块请求处理
 */
@Singleton
@Named
@Controller(ModuleId.Friend)
class FriendController @Inject constructor(
  private val friendService: FriendService
) : AbstractController(ModuleId.Friend) {

  @Cmd(1)
  fun getInfo(playerId: Long) = friendService.getInfo(playerId)

  @Cmd(2)
  fun getInfo2(playerId: Long) = friendService.getInfo2(playerId)

  @Cmd(3)
  fun getInfo3(playerId: Long) = RequestResult.async {
    PlayFuture.successful(ok(FriendInfo()))
  }
}
