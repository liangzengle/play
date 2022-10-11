package play.example.game.app.module.hero.entity

import play.entity.cache.CacheIndex
import play.example.game.app.module.player.entity.AbstractPlayerObjIdEntity
import play.example.game.app.module.player.entity.PlayerObjId

data class PlayerHeroId(@CacheIndex override val playerId: Long, val heroId: Int) : PlayerObjId()

/**
 *
 * @author LiangZengle
 */
class PlayerHeroEntity(id: PlayerHeroId) : AbstractPlayerObjIdEntity<PlayerHeroId>(id)
