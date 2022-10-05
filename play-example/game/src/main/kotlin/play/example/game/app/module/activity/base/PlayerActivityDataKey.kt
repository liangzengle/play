package play.example.game.app.module.activity.base

import play.example.game.app.module.activity.impl.login.data.LoginActivityData
import play.util.collection.SerializableAttributeKey

/**
 *
 * @author LiangZengle
 */
object PlayerActivityDataKey {

  val Login = SerializableAttributeKey.valueOf<LoginActivityData>("login")
}
