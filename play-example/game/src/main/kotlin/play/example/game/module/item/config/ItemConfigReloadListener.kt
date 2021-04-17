package play.example.game.module.item.config

import javax.inject.Singleton
import play.config.GenericConfigReloadListener

/**
 *
 * @author LiangZengle
 */
@Singleton
class ItemConfigReloadListener : GenericConfigReloadListener<ItemConfig>() {
  override fun onReload() {
    ItemConfigSet.list().forEach { println("reload: $it") }
  }
}
