package play.example.game.container.admin

import mu.KLogging
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.net.http.Action
import play.net.http.HttpActionManager
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author LiangZengle
 */
class ContainerAdminHttpActionManager : HttpActionManager() {
  companion object : KLogging()

  private val actionManagers = AtomicReference(IntObjectMaps.immutable.empty<HttpActionManager>())

  fun register(serverId: Int, manager: HttpActionManager) {
    actionManagers.getAndUpdate {
      it.newWithKeyValue(serverId, manager)
    }
  }

  fun unregister(serverId: Int) {
    actionManagers.getAndUpdate {
      it.newWithoutKey(serverId)
    }
  }

  override fun findAction(path: String, uri: String): Action? {
    throw UnsupportedOperationException()
  }

  fun findAction(serverId: Int, path: String, uri: String): Action? {
    return actionManagers.get().get(serverId)?.findAction(path, uri)
  }
}
