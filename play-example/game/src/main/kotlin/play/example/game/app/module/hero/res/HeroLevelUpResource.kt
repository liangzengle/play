package play.example.game.app.module.hero.res

import play.res.AbstractResource
import play.res.Grouped
import play.res.UniqueKey

class HeroLevelUpResource : AbstractResource(), Grouped<Int>, UniqueKey<Int> {

  val heroId = 0

  val level = 0

  override fun groupBy(): Int = heroId
  override fun key(): Int = 1
}
