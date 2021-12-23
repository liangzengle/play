package play.example.game.app.rpc

import org.springframework.beans.factory.config.BeanPostProcessor
import play.example.game.container.gs.domain.GameServerId
import play.example.game.container.rpc.AbstractRSocketServiceAnnotationProcessor
import play.example.game.container.rpc.ContainerRSocketServiceAnnotationProcessor

/**
 * @author LiangZengle
 */
class GameRSocketServiceAnnotationProcessor(
  private val underlying: ContainerRSocketServiceAnnotationProcessor, gameServerId: GameServerId
) : AbstractRSocketServiceAnnotationProcessor(gameServerId.toInt().toString(), ""), BeanPostProcessor {

  override fun addProvider(
    group: String, serviceName: String, version: String, serviceInterface: Class<*>, handler: Any
  ) {
    underlying.addProvider(group, serviceName, version, serviceInterface, handler)
  }
}
