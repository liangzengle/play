package play.example.game.app.module.hero.entity

import play.entity.cache.MultiEntityCacheKey
import play.example.game.app.module.player.entity.AbstractPlayerMultiEntity
import play.example.game.app.module.player.entity.PlayerObjId

data class PlayerHeroId(@MultiEntityCacheKey override val playerId: Long, val heroId: Int) : PlayerObjId()

/**
 *
 * @author LiangZengle
 */
class PlayerHeroEntity(id: PlayerHeroId) : AbstractPlayerMultiEntity<PlayerHeroId>(id) {


}
