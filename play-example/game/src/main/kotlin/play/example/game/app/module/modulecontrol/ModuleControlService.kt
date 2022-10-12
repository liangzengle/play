package play.example.game.app.module.modulecontrol

import org.eclipse.collections.api.set.primitive.IntSet
import org.springframework.stereotype.Component
import play.example.game.app.module.modulecontrol.entity.PlayerModuleControlEntityCache
import play.example.game.app.module.modulecontrol.event.PlayerModuleOpenEvent
import play.example.game.app.module.modulecontrol.res.ModuleControlResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.condition.subscribeAll
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.container.net.Session

/**
 *
 * @author LiangZengle
 */
@Component
class ModuleControlService(
  private val playerConditionService: PlayerConditionService,
  private val entityCache: PlayerModuleControlEntityCache,
  private val eventBus: PlayerEventBus
) {
  init {
    eventBus.subscribeAll(ModuleControlResourceSet.list(), { it.conditions }, ::checkOpen)
  }

  fun isOpen(self: Self, id: Int): Boolean = isOpen(self.id, id)

  fun isOpen(playerId: Long, id: Int): Boolean = entityCache.getOrNull(playerId)?.isOpen(id) ?: false

  fun getOpenedModules(self: Self): IntSet = entityCache.getOrCreate(self.id).getOpened()

  private fun checkOpen(self: Self, event: PlayerEvent) {
    val entity = entityCache.getOrCreate(self.id)
    for (moduleOpenConfig in ModuleControlResourceSet.list()) {
      val moduleId = moduleOpenConfig.id
      if (entity.isOpen(moduleId)) {
        continue
      }
      if (moduleOpenConfig.conditions.any { playerConditionService.check(self, it).isErr() }) {
        continue
      }
      entity.setOpen(moduleId)
      eventBus.publish(PlayerModuleOpenEvent(self.id, moduleId))
      Session.write(self.id, ModuleControlModule.moduleOpenPush(moduleId))
    }
  }
}
