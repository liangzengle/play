package play.example.module.reward

import play.example.module.player.Self
import play.example.module.player.args.PlayerArgProvider
import play.example.module.player.args.PlayerArgs
import play.example.module.reward.config.RawReward
import play.example.module.reward.model.Reward
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RawRewardConverter @Inject constructor(private val injector: Injector) : PostConstruct {
  private lateinit var playerArgProviders: Map<String, PlayerArgProvider>

  override fun postConstruct() {
    playerArgProviders = injector.instancesOf(PlayerArgProvider::class.java).toImmutableMap { it.key }
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
