package play.db.mongo.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.mongodb.MongoClientSettings
import com.typesafe.config.Config
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import javax.inject.Named
import javax.inject.Singleton
import play.db.Repository
import play.db.mongo.MongoClientSettingsProvider
import play.db.mongo.MongoDBRepository
import play.inject.guice.GuiceModule

/**
 *
 * Mongo Module
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class MongoGuiceModule : GuiceModule() {
  override fun configure() {
    bindDefaultProvider<MongoClientSettings, MongoClientSettingsProvider>()
    bindSingleton<MongoDBRepository>()
    overrideDefaultBinding<Repository, MongoDBRepository>()
  }

  @Provides
  @Singleton
  @Named("mongodb")
  fun config(config: Config): Config {
    return config.getConfig("mongodb")
  }

  @Provides
  @Singleton
  @Named("mongo")
  fun eventLoopGroup(@Named("mongodb") config: Config): EventLoopGroup {
    val nThread = config.getInt("client-threads")
    val threadFactory = DefaultThreadFactory("mongo")
    val usingEpoll = Epoll.isAvailable()
    return if (usingEpoll) {
      EpollEventLoopGroup(nThread, threadFactory)
    } else {
      NioEventLoopGroup(nThread, threadFactory)
    }
  }
}
