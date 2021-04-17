package play.example.game.base

import akka.actor.typed.ActorRef
import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Singleton
import play.akka.resumeSupervisor
import play.db.QueryService
import play.example.common.inject.ActorSystemProvider
import play.example.common.inject.AkkaGuiceModule
import play.example.game.base.net.SessionManager
import play.example.game.module.account.AccountManager
import play.example.game.module.account.entity.AccountEntityCache
import play.example.game.module.friend.FriendManager
import play.example.game.module.guild.GuildManager
import play.example.game.module.guild.entity.GuildEntityCache
import play.example.game.module.platform.PlatformServiceProvider
import play.example.game.module.player.PlayerEntityCacheInitializer
import play.example.game.module.player.PlayerManager
import play.example.game.module.player.PlayerRequestHandler
import play.example.game.module.player.PlayerService
import play.example.game.module.player.event.PlayerEventBus
import play.example.game.module.player.event.PlayerEventDispatcher
import play.example.game.module.player.event.PlayerEventDispatcherProvider
import play.example.game.module.server.ServerService
import play.example.game.module.server.config.ServerConfig
import play.example.game.module.server.config.ServerConfigProvider
import play.example.game.module.task.TaskEventReceiver
import play.scheduling.Scheduler

@AutoService(Module::class)
class GameGuiceModule : AkkaGuiceModule() {
  override fun configure() {
    super.configure()
    bindProvider<ServerConfig, ServerConfigProvider>()
    bindProvider<PlayerEventDispatcher, PlayerEventDispatcherProvider>()
  }

  @Singleton
  @Provides
  private fun sessionManager(systemProvider: ActorSystemProvider): ActorRef<SessionManager.Command> {
    return spawn(systemProvider, SessionManager.create(), "SessionManager")
  }

  @Singleton
  @Provides
  private fun accountManager(
    systemProvider: ActorSystemProvider,
    platformServiceProvider: PlatformServiceProvider,
    queryService: QueryService,
    sessionManager: ActorRef<SessionManager.Command>,
    serverService: ServerService,
    accountCache: AccountEntityCache,
    playerManager: ActorRef<PlayerManager.Command>,
    playerEntityCacheInitializer: PlayerEntityCacheInitializer
  ): ActorRef<AccountManager.Command> {
    return spawn(
      systemProvider,
      AccountManager.create(
        platformServiceProvider,
        queryService,
        sessionManager,
        serverService,
        accountCache,
        playerManager,
        playerEntityCacheInitializer
      ),
      "AccountManager"
    )
  }

  @Singleton
  @Provides
  private fun playerManager(
    systemProvider: ActorSystemProvider,
    eventDispatcher: PlayerEventDispatcher,
    queryService: QueryService,
    playerService: PlayerService,
    requestHandler: PlayerRequestHandler,
    scheduler: Scheduler,
    taskEventReceiver: TaskEventReceiver
  ): ActorRef<PlayerManager.Command> {
    return spawn(
      systemProvider,
      resumeSupervisor(
        PlayerManager.create(
          eventDispatcher,
          queryService,
          playerService,
          requestHandler,
          scheduler,
          taskEventReceiver
        )
      ),
      "PlayerManager"
    )
  }

  @Singleton
  @Provides
  private fun friendManager(
    systemProvider: ActorSystemProvider,
    requestHandler: PlayerRequestHandler
  ): ActorRef<FriendManager.Command> {
    return spawn(systemProvider, FriendManager.create(requestHandler), "FriendManager")
  }

  @Singleton
  @Provides
  private fun guildManager(
    systemProvider: ActorSystemProvider,
    guildEntityCache: GuildEntityCache,
    requestHandler: PlayerRequestHandler,
    playerService: PlayerService,
    playerEventBus: PlayerEventBus
  ): ActorRef<GuildManager.Command> {
    return spawn(
      systemProvider,
      GuildManager.create(requestHandler, guildEntityCache, playerService, playerEventBus),
      "GuildManager"
    )
  }
}
