package play.example.game.app.module.item

import play.example.game.app.module.player.Self
import play.example.game.container.command.Arg
import play.example.game.container.command.Command
import play.example.game.container.command.CommandModule

/**
 * 物品GM
 * @author LiangZengle
 */
@CommandModule(name = "Item", label = "W物品")
class ItemCommandModule {

  @Command(desc = "添加物品")
  private fun addItem(self: Self, @Arg("物品id") itemId: Int, @Arg("数量", "1") num: Int) {
  }
}
