package play.example.game.app.module.item.controller

import play.example.common.StatusCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.item.ItemService
import play.example.game.app.module.player.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 物品模块逻辑处理
 */
@Singleton
@Named
@Controller(ModuleId.Item)
class ItemController @Inject constructor(
  private val itemService: ItemService
) : AbstractController(ModuleId.Item) {

  @Cmd(1)
  fun useItem1(self: Self, itemUid: Int, num: Int) = RequestResult {
    StatusCode.Failure
  }

  @Cmd(2)
  fun useItem2(self: Self, itemUid: Int, num: Int) = RequestResult {
    itemService.useItem(self, itemUid, num)
  }
}
