package play.example.game.app.module.item

import org.springframework.stereotype.Component
import play.example.game.app.module.player.Self
import play.example.game.container.gm.GmCommandModule

/**
 * 物品GM
 * @author LiangZengle
 */
@Component
class ItemGM : GmCommandModule() {
  override val name: String get() = "Item"

  @Cmd(desc = "添加物品")
  private fun addItem(self: Self, @Arg("物品id") itemId: Int, @Arg("数量", "1") num: Int) {
  }
}
