package play.example.module.item.controller

import javax.inject.Inject
import javax.inject.Singleton
import play.example.module.ModuleId
import play.example.module.StatusCode
import play.example.module.item.ItemService
import play.example.module.player.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

/**
 * 物品模块逻辑处理
 */
@Singleton
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
