package play.example.game.app.module.activity.base

import akka.actor.typed.ActorRef
import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.server.ServerService
import play.res.AbstractResource
import play.res.ResourceReloadListener
import play.spring.OrderedSmartInitializingSingleton

/**
 *
 * @author LiangZengle
 */
@Component
class ActivityService(
  private val activityManager: ActorRef<ActivityManager.Command>,
  private val serverService: ServerService
) : ResourceReloadListener, OrderedSmartInitializingSingleton {

  override fun onResourceReloaded(reloadedResources: Set<Class<out AbstractResource>>) {
    if (reloadedResources.contains(ActivityResource::class.java)) {
      activityManager.tell(ActivityManager.ResourceReloaded)
    }
  }

  override fun afterSingletonsInstantiated() {
    if (serverService.isOpen()) {
      activityManager.tell(ActivityManager.Init)
    }
  }
}
