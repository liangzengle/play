package play.example.game.app.module.friend.entity

import org.eclipse.collections.impl.factory.primitive.LongSets
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

/**
 * 好友
 * @author LiangZengle
 */
class PlayerFriendEntity(id: Long) : AbstractPlayerLongIdEntity(id) {

  private var friends = LongSets.mutable.empty()

  fun isFriend(targetId: Long) = friends.contains(targetId)

  fun addFriend(targetId: Long) = friends.add(targetId)

  fun removeFriend(targetId: Long) = friends.remove(targetId)

  override fun initialize() {
    friends.removeIf { !PlayerManager.isPlayerExists(it) }
  }
}
