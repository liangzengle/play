package play.example.module.friend.controller

import play.mvc.Message
import play.util.collection.EmptyByteArray

/**
 *
 * @author LiangZengle
 */
class FriendInfo : Message {
  override fun encodeToByteArray(): ByteArray {
    return EmptyByteArray
  }
}
