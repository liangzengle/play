package play.example.module.item

import play.example.module.item.config.ItemConfigSet
import play.example.module.item.domain.ItemType
import play.example.module.player.Self
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 物品模块逻辑处理
 */
@Singleton
class ItemService @Inject constructor() {

  fun test() {
    ItemConfigSet.getGroupOrThrow(ItemType.Normal).list()
  }

  fun useItem(self: Self, itemUid: Int, num: Int): Int {
    return 0
  }
}
