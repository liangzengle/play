package play.example.game.app.module.hero

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.hero.entity.PlayerHeroEntity
import play.example.game.app.module.hero.entity.PlayerHeroEntityCache
import play.example.game.app.module.hero.entity.PlayerHeroId
import play.example.game.app.module.player.PlayerManager.Self

/**
 *
 * @author LiangZengle
 */
@Component
class HeroService @Autowired constructor(val heroEntityCache: PlayerHeroEntityCache) {

  private fun createHero(self: Self, heroId: Int): PlayerHeroEntity {
    return heroEntityCache.getOrCreate(PlayerHeroId(self.id, heroId))
  }

  fun listHeroEntities(self: Self): List<PlayerHeroEntity> {
    return heroEntityCache.getByIndex(self.id)
  }
}
