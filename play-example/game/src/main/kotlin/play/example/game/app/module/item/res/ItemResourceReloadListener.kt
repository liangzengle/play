package play.example.game.app.module.item.res

import org.springframework.stereotype.Component
import play.res.GenericResourceReloadListener

/**
 *
 * @author LiangZengle
 */
@Component
class ItemResourceReloadListener : GenericResourceReloadListener<ItemResource>() {
  override fun onReload() {
    ItemResourceSet.list().forEach { println("reload: $it") }
  }
}
