package play.example.module.item

import play.example.common.gm.GmCommand
import play.example.common.gm.GmCommandArg
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

  @GmCommand("add")
  private fun addItem(self: Self, @GmCommandArg("物品id") itemId: Int, @GmCommandArg("数量") num: Int) {
  }
}
