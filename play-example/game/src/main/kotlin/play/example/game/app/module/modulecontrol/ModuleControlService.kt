package play.example.game.app.module.modulecontrol

import org.eclipse.collections.api.set.primitive.IntSet
import org.springframework.stereotype.Component
import play.example.game.app.module.modulecontrol.entity.PlayerModuleControlEntityCache
import play.example.game.app.module.modulecontrol.event.PlayerModuleOpenEvent
import play.example.game.app.module.modulecontrol.res.ModuleOpenResource
import play.example.game.app.module.modulecontrol.res.ModuleOpenResourceSet
import play.example.game.app.module.player.Self
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.condition.listenConditionEvents
import play.example.game.app.module.player.event.*
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
) : PlayerEventListener {
  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .listenConditionEvents(ModuleOpenResourceSet.list(), ModuleOpenResource::conditions, ::checkOpen)
      .build()
  }

  fun isOpen(self: Self, id: Int): Boolean = isOpen(self.id, id)

  fun isOpen(playerId: Long, id: Int): Boolean = entityCache.getOrNull(playerId)?.isOpen(id) ?: false

  fun getOpenedModules(self: Self): IntSet = entityCache.getOrCreate(self.id).getOpened()

  private fun checkOpen(self: Self, event: PlayerEvent) {
    val entity = entityCache.getOrCreate(self.id)
    for (moduleOpenConfig in ModuleOpenResourceSet.list()) {
      val moduleId = moduleOpenConfig.id
      if (entity.isOpen(moduleId)) {
        continue
      }
      if (moduleOpenConfig.conditions.any { playerConditionService.check(self, it).isErr() }) {
        continue
      }
      entity.setOpen(moduleId)
      eventBus.post(PlayerModuleOpenEvent(self.id, moduleId))
      Session.write(self.id, ModuleControlModule.moduleOpenPush(moduleId))
    }
  }
}
