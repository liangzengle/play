package play.example.game.app.module.account

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.QueryService
import play.example.game.app.module.account.entity.AccountEntityCache
import play.example.game.app.module.platform.PlatformServiceProvider
import play.example.game.app.module.player.PlayerEntityCacheInitializer
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.server.ServerService
import play.example.game.container.gs.GameServerScopeConfiguration
import play.example.game.container.login.LoginDispatcherActor

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class AccountConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun accountManager(
    platformServiceProvider: PlatformServiceProvider,
    queryService: QueryService,
    loginDispatcher: ActorRef<LoginDispatcherActor.Command>,
    serverService: ServerService,
    accountCache: AccountEntityCache,
    playerManager: ActorRef<PlayerManager.Command>,
    playerEntityCacheInitializer: PlayerEntityCacheInitializer
  ): ActorRef<AccountManager.Command> {
    return spawn(
      AccountManager.create(
        platformServiceProvider,
        queryService,
        loginDispatcher,
        serverService,
        accountCache,
        playerManager,
        playerEntityCacheInitializer
      ), "AccountManager"
    )
  }
}
