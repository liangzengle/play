package play.db.mongo

import com.google.inject.Provides
import com.mongodb.MongoClientSettings
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import play.Configuration
import play.db.DbExecutor
import play.db.Repository
import play.inject.guice.GuiceModule
import play.util.reflect.classOf

class MongoGuiceModule : GuiceModule() {
  override fun configure() {
    val conf = ctx.conf.getConfiguration("db.mongodb")
    bind<Configuration>().qualifiedWith("mongodb").toInstance(conf)
    bind<Repository>().qualifiedWith("mongodb").to<MongoDBRepository>()
    optionalBind<MongoClientSettings.Builder>().setDefault().toProvider(classOf<MongoClientSettingBuilderProvider>())
    bind<MongoClientSettings>().toProvider(classOf<MongoClientSettingProvider>())
    optionalBind<DbExecutor>().setBinding().toProvider(MongoDbExecutorProvider::class.java)
  }

  @Provides
  @Singleton
  @Named("mongo")
  private fun eventLoopGroup(): EventLoopGroup {
    val nThread = ctx.conf.getInt("db.thread-pool-size")
    val threadFactory = DefaultThreadFactory("mongo")
    val usingEpoll = Epoll.isAvailable()
    return if (usingEpoll) {
      EpollEventLoopGroup(nThread, threadFactory)
    } else {
      NioEventLoopGroup(nThread, threadFactory)
    }
  }
}

@Singleton
private class MongoDbExecutorProvider @Inject constructor(@Named("mongo") eventLoopGroup: EventLoopGroup) :
  Provider<DbExecutor> {
  private val executor = DbExecutor(eventLoopGroup)
  override fun get(): DbExecutor = executor
}
