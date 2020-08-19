package play.example.game.app.module.reward

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.args.PlayerArgProvider
import play.example.game.app.module.player.args.PlayerArgs
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.res.RawReward
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.unsafeLazy

@Component
class RawRewardConverter(private val injector: PlayInjector) {
  private val playerArgProviders by unsafeLazy {
    injector.getInstancesOfType(PlayerArgProvider::class.java).toImmutableMap { it.key }
  }

  fun toReward(self: Self, rawReward: RawReward): Reward {
    return rawReward.toReward(PlayerArgs(self, playerArgProviders))
  }

  fun toReward(self: Self, rawRewards: Collection<RawReward>): List<Reward> {
    if (rawRewards.isEmpty()) {
      return emptyList()
    }
    val args = PlayerArgs(self, playerArgProviders)
    return rawRewards.map { it.toReward(args) }
  }
}
