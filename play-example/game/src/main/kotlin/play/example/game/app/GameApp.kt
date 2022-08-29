package play.example.game.app

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.Behaviors
import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.DefaultGracefullyShutdown
import play.GracefullyShutdown
import play.akka.scheduling.ActorScheduler
import play.db.mongo.Mongo
import play.entity.Entity
import play.entity.PlayEntityCacheConfiguration
import play.entity.cache.DefaultEntityCachePersistFailOver
import play.entity.cache.EntityCacheManager
import play.entity.cache.EntityCachePersistFailOver
import play.event.EnableEventBus
import play.example.common.Role
import play.example.game.container.command.CommandManager
import play.example.game.container.command.CommandService
import play.example.game.container.gs.GameServerScopeConfiguration
import play.example.game.container.gs.domain.GameServerId
import play.inject.PlayInjector
import play.inject.SpringPlayInjector
import play.mongodb.MongoDBRepositoryCustomizer
import play.rsocket.client.RSocketClientAutoConfiguration
import play.scheduling.Scheduler
import play.util.classOf
import play.util.concurrent.PlayFuture
import play.util.reflect.ClassgraphClassScanner
import play.rsocket.client.RSocketClientCustomizer as RSocketClientCustomizer1

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Import(value = [PlayEntityCacheConfiguration::class, RSocketClientAutoConfiguration::class])
@EnableEventBus
@Configuration(proxyBeanMethods = false)
class GameApp : GameServerScopeConfiguration() {

  @Bean
  fun idAndRole(gameServerId: GameServerId): RSocketClientCustomizer1 {
    val id = gameServerId.toInt()
    return RSocketClientCustomizer1 { builder ->
      builder.id(id).role(Role.Game)
    }
  }

//  @Bean
//  fun rsocketResume(gameServerId: GameServerId, address: HostAndPort): RSocketClientCustomizer {
//    val id = gameServerId.toInt()
//    val token = "$id@${address.host}:${address.port}"
//    return RSocketClientCustomizer { builder ->
//      builder.customizeConnector { connector ->
//        connector.resume(Resume().token { Unpooled.wrappedBuffer(token.encodeToByteArray()) })
//      }
//    }
//  }

  @Bean
  fun actorScheduler(
    scheduler: Scheduler
  ): ActorRef<ActorScheduler.Command> {
    return spawn("ActorScheduler", classOf()) { _ ->
      Behaviors.setup {
        ActorScheduler(
          it,
          scheduler
        )
      }
    }
  }

  @Bean
  fun gmCommandService(injector: PlayInjector, invokerManager: CommandManager): CommandService {
    return CommandService(injector, invokerManager)
  }

  @Bean
  fun playInjector(applicationContext: ApplicationContext): PlayInjector {
    return SpringPlayInjector(applicationContext)
  }

  @Bean
  fun entityCachePersistFailOver(gameServerId: GameServerId): EntityCachePersistFailOver {
    return DefaultEntityCachePersistFailOver("entity_back_up/${gameServerId.toInt()}")
  }

  @Bean
  fun mongoDBIndexCreator(classScanner: ClassgraphClassScanner): MongoDBRepositoryCustomizer {
    return MongoDBRepositoryCustomizer { repository ->
      val entityClasses = classScanner.getInstantiatableSubclassInfoList(Entity::class.java)
        .filter { it.packageName.startsWith(this.javaClass.packageName) }
        .loadClasses(Entity::class.java)
      Mongo.ensureIndexes(repository, entityClasses)
    }
  }

  /**
   * game application should use the [GracefullyShutdown] in container
   */
  @Bean(destroyMethod = "run")
  fun gracefullyShutdown(
    config: Config,
    scheduler: Scheduler,
    entityCacheManager: EntityCacheManager,
    gameServerId: GameServerId,
    phases: GracefullyShutdown.Phases
  ): GracefullyShutdown {
    val shutdown = DefaultGracefullyShutdown("GameApp(${gameServerId.toInt()})", phases, false, null)
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_SCHEDULER, GracefullyShutdown.PHASE_SHUTDOWN_SCHEDULER, scheduler
    ) { PlayFuture { if (it is AutoCloseable) it.close() } }
    shutdown.addTask(
      GracefullyShutdown.PHASE_FLUSH_ENTITY_CACHE, GracefullyShutdown.PHASE_FLUSH_ENTITY_CACHE, entityCacheManager
    ) { PlayFuture { if (it is AutoCloseable) it.close() } }
    return shutdown
  }
}
