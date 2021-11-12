package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import play.example.game.container.gs.logging.ActorMDC
import play.util.concurrent.PlayPromise
import play.util.unsafeCast
import play.util.unsafeLazy
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
abstract class GameServerScopeConfiguration : ApplicationContextAware {

  private lateinit var applicationContext: ApplicationContext

  private val gameServer: ActorRef<GameServerActor.Command> by unsafeLazy {
    applicationContext.getBean("gameServerActor").unsafeCast()
  }

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.applicationContext = applicationContext
  }

  protected fun <T> spawn(name: String, behaviorCreation: (ActorMDC) -> Behavior<T>): ActorRef<T> {
    val promise = PlayPromise.make<ActorRef<T>>()
    gameServer.tell(GameServerActor.Spawn(behaviorCreation, name, promise))
    return promise.future.get(Duration.ofSeconds(10))
  }
}
