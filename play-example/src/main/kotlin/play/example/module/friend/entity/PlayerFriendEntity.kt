package play.example.module.friend.entity

import org.eclipse.collections.api.block.predicate.primitive.LongPredicate
import org.eclipse.collections.impl.factory.primitive.LongSets
import play.example.module.player.entity.PlayerEntity

/**
 * 好友
 * @author LiangZengle
 */
class PlayerFriendEntity(id: Long) : PlayerEntity(id) {

  private var friends = LongSets.mutable.empty()

  fun isFriend(targetId: Long) = friends.contains(targetId)

  fun addFriend(targetId: Long) = friends.add(targetId)

  fun removeFriend(targetId: Long) = friends.remove(targetId)

  fun cleanUp(removalFilter: LongPredicate) {
    friends.removeIf(removalFilter)
  }
}

