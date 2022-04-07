package play.db

import com.typesafe.config.Config
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.GracefullyShutdown
import play.util.concurrent.PlayFuture
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayDBConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun tableNameFormatter(): TableNameFormatter {
    return LowerUnderscoreFormatter()
  }

  @Bean
  @ConditionalOnMissingBean
  fun tableNameResolver(config: Config, tableNameFormatter: TableNameFormatter): TableNameResolver {
    val postfixes = config.getStringList("play.db.table-name-trim-postfixes")
    return TableNameResolver(postfixes, tableNameFormatter)
  }

  @Bean
  @ConditionalOnMissingBean(name = ["dbExecutor"])
  fun dbExecutor(config: Config, shutdown: GracefullyShutdown): Executor {
    val nThread = config.getInt("play.db.thread-pool-size")
    val threadFactory = DefaultThreadFactory("db-executor")
    val executor = ThreadPoolExecutor(
      nThread, nThread, 0L,
      TimeUnit.MILLISECONDS,
      LinkedBlockingQueue(),
      threadFactory
    )
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_DATABASE_SERVICE,
      GracefullyShutdown.PHASE_SHUTDOWN_DATABASE_SERVICE,
      executor
    ) {
      PlayFuture {
        it.shutdown()
        it.awaitTermination(1, TimeUnit.SECONDS)
      }
    }
    return executor
  }
}
