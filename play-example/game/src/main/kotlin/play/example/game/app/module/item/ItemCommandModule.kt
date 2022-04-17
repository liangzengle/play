package play.example.game.app.module.item

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.container.command.Command
import play.example.game.container.command.CommandModule
import play.example.game.container.command.Param

/**
 * 物品GM
 * @author LiangZengle
 */
@CommandModule(name = "Item", label = "W物品")
class ItemCommandModule {

  @Command(desc = "添加物品")
  private fun addItem(self: Self, @Param("物品id") itemId: Int, @Param("数量", "1") num: Int) {
  }
}
