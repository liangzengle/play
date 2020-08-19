package play.example.module.friend.controller

import io.vavr.concurrent.Future
import play.example.module.ModuleId
import play.example.module.friend.FriendService
import play.mvc.*
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
  
  @NotPlayerThread
  @Cmd(1)
  fun getInfo(playerId: Long) = RequestResult {
    friendService.getInfo(playerId)
  }

  @NotPlayerThread
  @Cmd(2)
  fun getInfo2(playerId: Long) = RequestResult {
    friendService.getInfo2(playerId)
  }

  @NotPlayerThread
  @Cmd(3)
  fun getInfo3(playerId: Long) = RequestResult {
    Future.successful(RequestResult.Ok(FriendInfo()))
  }
}
