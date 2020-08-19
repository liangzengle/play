package play.example.game.app.module.item

import org.springframework.stereotype.Component
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.item.res.ItemResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.util.control.Result2
import play.util.control.ok

/**
 * 物品模块逻辑处理
 */
@Component
class ItemService() {

  fun test() {
    ItemResourceSet.getGroupOrThrow(ItemType.Normal).list()
  }

  fun useItem(self: Self, itemUid: Int, num: Int): Result2<Unit> {
    return ok()
  }
}
