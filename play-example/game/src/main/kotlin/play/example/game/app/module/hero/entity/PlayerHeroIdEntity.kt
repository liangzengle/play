package play.example.game.app.module.hero.entity

import org.eclipse.collections.impl.factory.primitive.IntSets
import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

/**
 *
 * @author LiangZengle
 */
class PlayerHeroIdEntity(id: Long) : AbstractPlayerLongIdEntity(id) {

  var heroIds = IntSets.immutable.empty()

  fun addHero(heroId: Int) {
    if (!heroIds.contains(heroId)) {
      heroIds = heroIds.newWith(heroId)
    }
  }
}
