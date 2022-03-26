package play.example.game.app.module.item

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

/**
 * 物品模块逻辑处理
 */
@Component
@Controller(ModuleId.Item)
class ItemController(
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
