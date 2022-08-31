package play.akka.serialization

import akka.actor.ExtendedActorSystem
import io.altoo.akka.serialization.kryo.DefaultKryoInitializer
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaKryo
import play.kryo.PlayKryoCustomizer

/**
 *
 *
 * @author LiangZengle
 */
class KryoInitializer : DefaultKryoInitializer() {

  override fun postInit(kryo: ScalaKryo, system: ExtendedActorSystem) {
    super.postInit(kryo, system)
    PlayKryoCustomizer.customize(kryo)
  }
}
