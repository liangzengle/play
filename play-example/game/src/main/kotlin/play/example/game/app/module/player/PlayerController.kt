package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.subscribe
import play.example.game.container.command.CommandService
import play.example.player.message.PlayerProto
import play.example.reward.message.RewardResultSetProto
import play.mvc.*
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
@Controller(ModuleId.Player)
@Component
class PlayerController(
  private val service: PlayerService,
  private val commandService: CommandService,
  private val eventBus: PlayerEventBus
) : AbstractController(ModuleId.Player) {

  init {
    eventBus.subscribe<PingEvent> { self, event ->
      println("$self ping")
    }
  }

  private data class PingEvent(override val playerId: Long) : PlayerEvent

  @Cmd(1, dummy = true)
  fun create(name: String): RequestResult<Boolean> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(): RequestResult<PlayerProto> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(self: PlayerManager.Self, msg: String): RequestResult<String> {
    eventBus.publish(PingEvent(self.id))
    return ok("world!").toRequestResult()
  }

  @Cmd(4)
  fun changeName(self: PlayerManager.Self, newName: String): RequestResult<String> = RequestResult.async {
    service.changeName(self, newName)
  }

  /**
   * 推送消息
   */
  @Cmd(101)
  lateinit var StringMessage: Push<String>

  /**
   * 推送奖励或者消耗结果
   */
  @Cmd(102)
  lateinit var RewardOrCost: Push<RewardResultSetProto>
}
