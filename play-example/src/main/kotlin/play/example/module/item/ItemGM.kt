package play.example.module.item

import play.example.common.gm.GmCommandModule
import play.example.module.player.Self
import javax.inject.Singleton

/**
 * 物品GM
 * @author LiangZengle
 */
@Singleton
class ItemGM : GmCommandModule() {
  override val name: String get() = "Item"

  @Cmd(desc = "添加物品")
  private fun addItem(self: Self, @Arg("物品id") itemId: Int, @Arg("数量", "1") num: Int) {
  }
}
