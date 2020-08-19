package play.example.game.app.rpc

import org.springframework.beans.factory.config.BeanPostProcessor
import play.example.game.container.gs.domain.GameServerId
import play.rsocket.rpc.AbstractRSocketServiceAnnotationProcessor
import play.rsocket.rpc.RSocketServiceAnnotationProcessor

/**
 * @author LiangZengle
 */
class GameRSocketServiceAnnotationProcessor(
  private val underlying: RSocketServiceAnnotationProcessor, gameServerId: GameServerId
) : AbstractRSocketServiceAnnotationProcessor(gameServerId.toInt().toString(), ""), BeanPostProcessor {

  override fun addProvider(
    group: String, serviceName: String, version: String, serviceInterface: Class<*>, handler: Any
  ) {
    underlying.addProvider(group, serviceName, version, serviceInterface, handler)
  }
}
