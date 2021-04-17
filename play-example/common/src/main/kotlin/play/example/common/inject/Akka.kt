package play.example.common.inject

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.Adapter
import com.google.inject.Provides
import com.typesafe.config.Config
import java.time.Duration
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.ShutdownCoordinator
import play.example.common.akka.scheduling.AkkaScheduler
import play.inject.guice.GuiceModule
import play.scala.await
import play.scheduling.PlayScheduler
import scala.concurrent.ExecutionContextExecutor

abstract class AkkaGuiceModule : GuiceModule() {
  override fun configure() {
//    binder().bind(ActorSystem::class.java).toProvider(ActorSystemProvider::class.java).`in`(Scopes.SINGLETON)
    bindProvider<ActorSystem<*>, ActorSystemProvider>()
    bindSingleton<AkkaScheduler>()
    overrideDefaultBinding<PlayScheduler, AkkaScheduler>()
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

class ActorSystemProvider @Inject constructor(conf: Config, shutdownCoordinator: ShutdownCoordinator) :
  Provider<ActorSystem<*>> {
  private val system = Adapter.toTyped(akka.actor.ActorSystem.create(conf.getString("actor-system-name")))

  init {
    shutdownCoordinator.addShutdownTask("Shutdown Actor System") {
      system.terminate()
      system.whenTerminated().await(Duration.ofMinutes(1))
    }
  }

  override fun get(): ActorSystem<*> = system
}
