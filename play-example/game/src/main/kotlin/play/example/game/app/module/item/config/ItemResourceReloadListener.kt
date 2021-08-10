package play.example.game.app.module.item.config

import play.res.GenericResourceReloadListener
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
@Named
class ItemResourceReloadListener : GenericResourceReloadListener<ItemResource>() {
  override fun onReload() {
    ItemResourceSet.list().forEach { println("reload: $it") }
  }
}
