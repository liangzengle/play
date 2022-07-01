package play.example.game.app.module.hero.entity

import play.entity.cache.CacheIndex
import play.entity.cache.InitialCacheSize
import play.example.game.app.module.player.entity.AbstractPlayerEntity

/**
 *
 * @author LiangZengle
 */
@InitialCacheSize("x100")
class PlayerHeroEntity(id: Long, val heroId: Int, @CacheIndex override val playerId: Long) : AbstractPlayerEntity(id)
