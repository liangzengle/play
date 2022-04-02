package play.mongodb

import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.typesafe.config.Config
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.GracefullyShutdown
import play.db.mongo.Mongo
import play.net.netty.toPlay

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Mongo::class)
class PlayMongoConfiguration {

  @Bean("dbExecutor")
  fun dbExecutor(config: Config, shutdown: GracefullyShutdown): EventLoopGroup {
    val nThread = config.getInt("play.mongodb.client-threads")
    val threadFactory = DefaultThreadFactory("mongo")
    val executor = if (Epoll.isAvailable()) {
      EpollEventLoopGroup(nThread, threadFactory)
    } else {
      NioEventLoopGroup(nThread, threadFactory)
    }
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_DATABASE_SERVICE,
      GracefullyShutdown.PHASE_SHUTDOWN_DATABASE_SERVICE,
      executor
    ) {
      it.shutdownGracefully().toPlay()
    }
    return executor
  }

  @Bean
  @ConditionalOnMissingBean
  fun mongoClientSettings(
    config: Config,
    @Qualifier("dbExecutor") eventLoopGroup: EventLoopGroup
  ): MongoClientSettings {
    val conf = config.getConfig("play.mongodb")
    return Mongo.newClientSettings(conf, eventLoopGroup)
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  fun mongoClient(settings: MongoClientSettings): MongoClient {
    return MongoClients.create(settings)
  }
}
