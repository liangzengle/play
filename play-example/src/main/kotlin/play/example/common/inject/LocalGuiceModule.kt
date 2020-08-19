package play.example.common.inject

import akka.actor.typed.ActorRef
import com.google.inject.Provides
import play.akka.resumeSupervisor
import play.db.QueryService
import play.example.common.net.NetGuiceModule
import play.example.common.net.SessionManager
import play.example.module.account.AccountManager
import play.example.module.account.entity.AccountCache
import play.example.module.friend.FriendManager
import play.example.module.platform.PlatformServiceProvider
import play.example.module.player.PlayerManager
import play.example.module.player.PlayerRequestHandler
import play.example.module.player.PlayerService
import play.example.module.player.event.PlayerEventDispatcher
import play.example.module.player.event.PlayerEventDispatcherProvider
import play.example.module.server.ServerService
import play.example.module.server.config.ServerConfig
import play.example.module.server.config.ServerConfigProvider
import play.util.scheduling.Scheduler
import javax.inject.Singleton

class LocalGuiceModule : AkkGuiceModule() {
  override fun configure() {
    val netModule = NetGuiceModule()
    netModule.initContext(ctx)
    install(netModule)

    bind<ServerConfig>().toProvider(ServerConfigProvider::class.java)
    bind<PlayerEventDispatcher>().toProvider(PlayerEventDispatcherProvider::class.java)
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
    accountCache: AccountCache,
    playerManager: ActorRef<PlayerManager.Command>
  ): ActorRef<AccountManager.Command> {
    return spawn(
      systemProvider,
      AccountManager.create(
        platformServiceProvider,
        queryService,
        sessionManager,
        serverService,
        accountCache,
        playerManager
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
    scheduler: Scheduler
  ): ActorRef<PlayerManager.Command> {
    return spawn(
      systemProvider,
      resumeSupervisor(
        PlayerManager.create(
          eventDispatcher,
          queryService,
          playerService,
          requestHandler,
          scheduler
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
}
