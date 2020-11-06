package play.example.module.item.config

import play.config.ConfigRefreshEvent
import play.config.GenericConfigRefreshListener
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
class ItemConfigRefreshListener : GenericConfigRefreshListener<ItemConfig, ConfigRefreshEvent>() {
  override fun onEvent(event: ConfigRefreshEvent) {
    ItemConfigSet.list().forEach { println((if (event.isLoad()) "load: " else "reload: ") + it) }
  }
}
