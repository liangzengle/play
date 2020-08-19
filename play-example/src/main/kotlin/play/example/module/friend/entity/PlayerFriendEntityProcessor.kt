package play.example.module.friend.entity

import play.db.EntityProcessor
import play.example.module.player.PlayerManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 好友处理不存在的玩家
 * @author LiangZengle
 */
@Singleton
class PlayerFriendEntityProcessor @Inject constructor() :
  EntityProcessor<PlayerFriendEntity>() {

  override fun postLoad(entity: PlayerFriendEntity) {
    super.postLoad(entity)
    entity.cleanUp { !PlayerManager.isPlayerExists(it) }
  }
}
