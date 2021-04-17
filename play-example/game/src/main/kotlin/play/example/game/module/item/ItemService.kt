package play.example.game.module.item

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.item.config.ItemConfigSet
import play.example.game.module.item.domain.ItemType
import play.example.game.module.player.Self
import play.util.control.Result2
import play.util.control.ok

/**
 * 物品模块逻辑处理
 */
@Singleton
class ItemService @Inject constructor() {

  fun test() {
    ItemConfigSet.getGroupOrThrow(ItemType.Normal).list()
  }

  fun useItem(self: Self, itemUid: Int, num: Int): Result2<Unit> {
    return ok()
  }
}
