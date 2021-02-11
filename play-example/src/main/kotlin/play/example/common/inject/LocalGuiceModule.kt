package play.example.common.inject

import akka.actor.typed.ActorRef
import com.google.inject.Provides
import javax.inject.Singleton
import play.akka.resumeSupervisor
import play.db.QueryService
import play.db.cache.CaffeineEntityCacheFactory
import play.db.cache.EntityCacheFactory
import play.example.common.admin.AdminGuiceModule
import play.example.common.net.NetGuiceModule
import play.example.common.net.SessionManager
import play.example.module.account.AccountManager
import play.example.module.account.entity.AccountEntityCache
import play.example.module.friend.FriendManager
import play.example.module.guild.GuildManager
import play.example.module.guild.entity.GuildEntityCache
import play.example.module.platform.PlatformServiceProvider
import play.example.module.player.PlayerEntityCacheInitializer
import play.example.module.player.PlayerManager
import play.example.module.player.PlayerRequestHandler
import play.example.module.player.PlayerService
import play.example.module.player.event.PlayerEventBus
import play.example.module.player.event.PlayerEventDispatcher
import play.example.module.player.event.PlayerEventDispatcherProvider
import play.example.module.server.ServerService
import play.example.module.server.config.ServerConfig
import play.example.module.server.config.ServerConfigProvider
import play.example.module.task.TaskEventReceiver
import play.util.scheduling.Scheduler

class LocalGuiceModule : AkkaGuiceModule() {
  override fun configure() {
    super.configure()
    install(NetGuiceModule())
    if (ctx.conf.getBoolean("admin.enabled")) {
      install(AdminGuiceModule())
    }

    bind<ServerConfig>().toProvider(ServerConfigProvider::class.java).asEagerSingleton()
    bind<PlayerEventDispatcher>().toProvider(PlayerEventDispatcherProvider::class.java)

    optionalBind<EntityCacheFactory>().to<CaffeineEntityCacheFactory>()
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
