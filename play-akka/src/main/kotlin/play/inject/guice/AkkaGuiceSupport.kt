package play.inject.guice

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.internal.adapter.ActorRefFactoryAdapter
import akka.actor.typed.javadsl.Behaviors
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.util.Types
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TypedActorRefProvider<T>(private val type: Class<T>, private val name: String) : Provider<ActorRef<T>> {

  @Inject
  private lateinit var actorSystem: ActorSystem

  @Inject
  private lateinit var injector: Injector

  private val ref: ActorRef<T> by lazy {
    val behavior = injector.getInstance(Key.get(behaviorOf(type)))
    ActorRefFactoryAdapter.spawn(
      actorSystem,
      Behaviors.supervise(behavior).onFailure(SupervisorStrategy.resume()),
      name,
      Props.empty(),
      false
    )
  }

  override fun get(): ActorRef<T> = ref
}

@Suppress("UNCHECKED_CAST")
fun <T> behaviorOf(type: Class<T>): TypeLiteral<Behavior<T>> {
  val parameterizedType = Types.newParameterizedType(Behavior::class.java, type)
  return TypeLiteral.get(parameterizedType) as TypeLiteral<Behavior<T>>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> behaviorOf(): TypeLiteral<Behavior<T>> {
  val parameterizedType = Types.newParameterizedType(Behavior::class.java, T::class.java)
  return TypeLiteral.get(parameterizedType) as TypeLiteral<Behavior<T>>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> actorOf(): TypeLiteral<ActorRef<T>> {
  val parameterizedType = Types.newParameterizedType(ActorRef::class.java, T::class.java)
  return TypeLiteral.get(parameterizedType) as TypeLiteral<ActorRef<T>>
}
