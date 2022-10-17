package play.example.game.app.module.activity.base

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.example.game.app.module.activity.impl.login.data.LoginActivityData
import play.example.game.app.module.task.entity.TaskData
import play.util.collection.SerializableAttributeKey

/**
 *
 * @author LiangZengle
 */
object PlayerActivityDataKey {

  @JvmStatic
  val Login = SerializableAttributeKey.valueOf<LoginActivityData>("Login")

  @JvmStatic
  val TaskData = SerializableAttributeKey.valueOf<MutableIntObjectMap<TaskData>>("TaskData")
}
