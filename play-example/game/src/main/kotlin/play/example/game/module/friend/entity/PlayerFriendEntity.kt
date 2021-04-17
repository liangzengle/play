package play.example.game.module.friend.entity

import org.eclipse.collections.impl.factory.primitive.LongSets
import play.example.game.module.player.PlayerManager
import play.example.game.module.player.entity.AbstractPlayerEntity

/**
 * 好友
 * @author LiangZengle
 */
class PlayerFriendEntity(id: Long) : AbstractPlayerEntity(id) {

  private var friends = LongSets.mutable.empty()

  fun isFriend(targetId: Long) = friends.contains(targetId)

  fun addFriend(targetId: Long) = friends.add(targetId)

  fun removeFriend(targetId: Long) = friends.remove(targetId)

  override fun initialize() {
    friends.removeIf { !PlayerManager.isPlayerExists(it) }
  }
}
