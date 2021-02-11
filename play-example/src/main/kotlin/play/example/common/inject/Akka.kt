package play.example.common.inject

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.Adapter
import com.google.inject.Provides
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.ApplicationLifecycle
import play.Configuration
import play.example.common.akka.scheduling.AkkaScheduler
import play.inject.guice.GuiceModule
import play.util.scheduling.PlayScheduler
import scala.concurrent.ExecutionContextExecutor

abstract class AkkaGuiceModule : GuiceModule() {
  override fun configure() {
    super.configure()
    bind<ActorSystem<*>>().toProvider(ActorSystemProvider::class.java)
    optionalBind<PlayScheduler>().to<AkkaScheduler>()
  }

  fun <T> spawn(systemProvider: ActorSystemProvider, behavior: Behavior<T>, name: String): ActorRef<T> {
    return Adapter.spawn(
      Adapter.toClassic(systemProvider.get()),
      behavior,
      name
    )
  }

  @Provides
  @Singleton
  private fun executionContext(actorSystemProvider: ActorSystemProvider): ExecutionContextExecutor {
    return actorSystemProvider.get().executionContext()
  }

  @Provides
  @Singleton
  private fun scheduler(actorSystemProvider: ActorSystemProvider): Scheduler {
    return actorSystemProvider.get().scheduler()
  }
}

@Singleton
class ActorSystemProvider @Inject constructor(conf: Configuration, lifecycle: ApplicationLifecycle) :
  Provider<ActorSystem<*>> {
  private val system = Adapter.toTyped(akka.actor.ActorSystem.create(conf.getString("actor-system-name")))

  init {
    lifecycle.addShutdownHook("Shutdown Actor System") {
      system.terminate() // TODO
    }
  }

  override fun get(): ActorSystem<*> = system
}
