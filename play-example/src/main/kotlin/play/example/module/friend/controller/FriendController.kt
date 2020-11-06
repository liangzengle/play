package play.example.module.friend.controller

import play.example.module.ModuleId
import play.example.module.friend.FriendService
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.concurrent.PlayFuture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 好友模块请求处理
 */
@Singleton
@Controller(ModuleId.Friend)
class FriendController @Inject constructor(
  private val friendService: FriendService
) : AbstractController(ModuleId.Friend) {

  @Cmd(1)
  fun getInfo(playerId: Long) = RequestResult {
    friendService.getInfo(playerId)
  }

  @Cmd(2)
  fun getInfo2(playerId: Long) = RequestResult {
    friendService.getInfo2(playerId)
  }

  @Cmd(3)
  fun getInfo3(playerId: Long) = RequestResult {
    PlayFuture.successful(RequestResult.Ok(FriendInfo()))
  }
}
