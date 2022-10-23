package play.example.game.app.module.activity.impl.rank.res

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import play.example.game.app.module.activity.impl.rank.event.KillMonsterPlayerEvent
import play.example.game.app.module.player.event.PlayerEvent
import play.util.classOf
import play.util.ranking.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface ActivityRankType<T : PlayerEvent> {

  fun eventType(): Class<T>

  fun toRankElement(event: T): SimpleRankingElementLong

  @JsonTypeName("KillMonster")
  data object KillMonster : ActivityRankType<KillMonsterPlayerEvent> {
    override fun eventType(): Class<KillMonsterPlayerEvent> {
      return classOf()
    }

    override fun toRankElement(event: KillMonsterPlayerEvent): SimpleRankingElementLong {
      return SimpleRankingElementLong(event.playerId, event.num.toLong())
    }
  }
}
