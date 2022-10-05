package play.example.game.app.module.activity.base

import play.util.collection.SerializableAttributeKey

/**
 *
 * @author LiangZengle
 */
object ActivityDataKey {

  @JvmStatic
  val None = SerializableAttributeKey.valueOf<Int>("none")

  @JvmStatic
  val Test = SerializableAttributeKey.valueOf<Int>("test")

  @JvmStatic
  val Login = SerializableAttributeKey.valueOf<Int>("login")
}
