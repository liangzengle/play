package play.example.game.app.module.item

import play.example.game.app.module.item.config.ItemResourceSet
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.player.Self
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 物品模块逻辑处理
 */
@Singleton
@Named
class ItemService @Inject constructor() {

  fun test() {
    ItemResourceSet.getGroupOrThrow(ItemType.Normal).list()
  }

  fun useItem(self: Self, itemUid: Int, num: Int): Result2<Unit> {
    return ok()
  }
}
