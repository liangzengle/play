package play.db

import com.google.common.util.concurrent.MoreExecutors
import play.ApplicationLifecycle
import play.Configuration
import play.getLogger
import play.util.concurrent.threadFactory
import java.time.Duration
import java.util.concurrent.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

class DbExecutor(executor: ExecutorService) : ExecutorService by executor


@Singleton
class DbExecutorProvider @Inject constructor(conf: Configuration, lifecycle: ApplicationLifecycle) :
  Provider<DbExecutor> {

  private val logger = getLogger()

  private val threadPoolSize = conf.getInt("db.thread-pool-size")

  private val executor = object : ThreadPoolExecutor(
    threadPoolSize,
    Int.MAX_VALUE,
    1,
    TimeUnit.SECONDS,
    LinkedBlockingQueue(),
    threadFactory("db-executor"),
    CallerRunsPolicy()
  ) {
    override fun afterExecute(r: Runnable?, t: Throwable?) {
      super.afterExecute(r, t)
      var ex = t
      if (ex == null && r is Future<*>) {
        try {
          if (r.isDone) r.get()
        } catch (e: CancellationException) {
          ex = e
        } catch (e: ExecutionException) {
          ex = e.cause
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }
      if (ex != null) logger.error(ex) { ex.message }
    }
  }

  private val dbExecutor = DbExecutor(executor)

  init {
    lifecycle.addShutdownHook("Shutdown db-executor", ApplicationLifecycle.PRIORITY_LOWEST) {
      MoreExecutors.shutdownAndAwaitTermination(dbExecutor, Duration.ofSeconds(60))
    }
  }

  override fun get(): DbExecutor = dbExecutor

}
