package play.example.game.app.module.hero

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.hero.entity.PlayerHeroEntity
import play.example.game.app.module.hero.entity.PlayerHeroEntityCache
import play.example.game.app.module.hero.entity.PlayerHeroId
import play.example.game.app.module.hero.entity.PlayerHeroIdEntityCache
import play.example.game.app.module.player.Self

/**
 *
 * @author LiangZengle
 */
@Component
class HeroService @Autowired constructor(
  val heroEntityCache: PlayerHeroEntityCache,
  val heroIdEntityCache: PlayerHeroIdEntityCache
) {

  private fun createHero(self: Self, heroId: Int): PlayerHeroEntity {
    val id = PlayerHeroId(self.id, heroId)
    val heroIdEntity = heroIdEntityCache.getOrCreate(self.id)
    return heroEntityCache.getOrCreate(id) { k ->
      val entity = PlayerHeroEntity(k)
      heroIdEntity.addHero(heroId)
      entity
    }
  }

  fun listHeroEntities(self: Self): List<PlayerHeroEntity> {
    return heroIdEntityCache.getOrCreate(self.id).heroIds
      .asLazy()
      .collect { heroId ->
        heroEntityCache.getOrNull(
          PlayerHeroId(
            self.id,
            heroId
          )
        )
      }
      .filterNotNull()
  }
}
