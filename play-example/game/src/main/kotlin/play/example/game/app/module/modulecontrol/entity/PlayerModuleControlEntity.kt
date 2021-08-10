package play.example.game.app.module.modulecontrol.entity

import org.eclipse.collections.impl.factory.primitive.IntSets
import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

/**
 *
 * @author LiangZengle
 */
class PlayerModuleControlEntity(id: Long) : AbstractPlayerLongIdEntity(id) {

  private val open = IntSets.mutable.empty()

  fun isOpen(id: Int) = open.add(id)

  fun setOpen(id: Int) {
    open.add(id)
  }

  fun getOpened() = open
}
