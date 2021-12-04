package play.example.game.app.module.friend.entity

import org.eclipse.collections.impl.factory.primitive.LongSets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.entity.cache.EntityInitializer
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.entity.AbstractPlayerEntity

/**
 * 好友
 * @author LiangZengle
 */
class PlayerFriendEntity(id: Long) : AbstractPlayerEntity(id) {

  private var friends = LongSets.mutable.empty()

  fun isFriend(targetId: Long) = friends.contains(targetId)

  fun addFriend(targetId: Long) = friends.add(targetId)

  fun removeFriend(targetId: Long) = friends.remove(targetId)

  fun removeIf(predicate: (Long) -> Boolean) {
    friends.removeIf(predicate)
  }
}

@Component
class PlayerFriendEntityInitializer @Autowired constructor(private val playerService: PlayerService) :
  EntityInitializer<PlayerFriendEntity>() {
  override fun beforeInitialize(entity: PlayerFriendEntity) {
    super.beforeInitialize(entity)
    entity.removeIf { !playerService.isPlayerExists(it) }
  }
}
