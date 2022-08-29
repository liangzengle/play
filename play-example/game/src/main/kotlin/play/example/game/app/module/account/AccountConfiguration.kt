package play.example.game.app.module.account

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.app.module.account.entity.AccountEntityCache
import play.example.game.app.module.platform.PlatformServiceProvider
import play.example.game.app.module.player.PlayerEntityCacheInitializer
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.server.ServerService
import play.example.game.container.gs.GameServerScopeConfiguration
import play.example.game.container.login.LoginDispatcherActor
import play.util.classOf

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class AccountConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun accountManager(
    platformServiceProvider: PlatformServiceProvider,
    accountIdCache: AccountIdCache,
    loginDispatcher: ActorRef<LoginDispatcherActor.Command>,
    serverService: ServerService,
    accountCache: AccountEntityCache,
    playerManager: ActorRef<PlayerManager.Command>,
    playerEntityCacheInitializer: PlayerEntityCacheInitializer,
    playerService: PlayerService
  ): ActorRef<AccountManager.Command> {
    return spawn("AccountManager", classOf()) { mdc ->
      AccountManager.create(
        platformServiceProvider,
        accountIdCache,
        loginDispatcher,
        serverService,
        accountCache,
        playerManager,
        playerEntityCacheInitializer,
        playerService,
        mdc
      )
    }
  }
}
