package play.example.game.container.admin

import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.net.http.Action
import play.net.http.HttpActionManager
import java.lang.ref.WeakReference
import java.util.*

/**
 *
 * @author LiangZengle
 */
class ContainerAdminHttpActionManager : HttpActionManager() {
  private var actionManagers = IntObjectMaps.immutable.empty<WeakReference<HttpActionManager>>()

  @Synchronized
  fun registerActionManager(serverId: Int, manager: HttpActionManager) {
    actionManagers = actionManagers.newWithKeyValue(serverId, WeakReference(manager))
  }

  override fun findAction(path: String, uri: String): Optional<Action> {
    throw UnsupportedOperationException()
  }

  fun findAction(serverId: Int, path: String, uri: String): Optional<Action> {
    val actionManager = actionManagers.get(serverId)?.get() ?: return Optional.empty()
    return actionManager.findAction(path, uri)
  }
}
