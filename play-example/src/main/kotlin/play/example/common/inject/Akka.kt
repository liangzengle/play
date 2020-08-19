package play.example.common.inject

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Adapter
import play.ApplicationLifecycle
import play.Configuration
import play.inject.guice.GuiceModule
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

abstract class AkkGuiceModule : GuiceModule() {
  fun <T> spawn(systemProvider: ActorSystemProvider, behavior: Behavior<T>, name: String): ActorRef<T> {
    return Adapter.spawn(
      Adapter.toClassic(systemProvider.get()),
      behavior,
      name
    )
  }
}

@Singleton
class ActorSystemProvider @Inject constructor(conf: Configuration, lifecycle: ApplicationLifecycle) :
  Provider<ActorSystem<*>> {
  private val system = Adapter.toTyped(akka.actor.ActorSystem.create(conf.getString("actor-system-name")))

  init {
    lifecycle.addShutdownHook("Shutdown ActorSystem") {
      system.terminate() // TODO
    }
  }

  override fun get(): ActorSystem<*> = system
}
