package play.example.game.app.module.player

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.app.module.player.event.PlayerEventDispatcher
import play.example.game.app.module.playertask.PlayerTaskEventReceiver
import play.example.game.container.gs.GameServerScopeConfiguration
import play.scheduling.Scheduler

@Configuration(proxyBeanMethods = false)
class PlayerSpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun playerManager(
    eventDispatcher: PlayerEventDispatcher,
    playerIdNameCache: PlayerIdNameCache,
    playerService: PlayerService,
    requestHandler: PlayerRequestHandler,
    scheduler: Scheduler,
    taskEventReceiver: PlayerTaskEventReceiver
  ): ActorRef<PlayerManager.Command> {
    return spawn("PlayerManager") { mdc ->
      PlayerManager.create(
        eventDispatcher,
        playerIdNameCache,
        playerService,
        requestHandler,
        scheduler,
        taskEventReceiver,
        mdc
      )
    }
  }
}
