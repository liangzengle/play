package play.example.game.app.admin

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.container.admin.ContainerAdminHttpActionManager
import play.example.game.container.gs.domain.GameServerId
import play.net.http.HttpActionManager

@Component
class AdminHttpActionManager @Autowired constructor(
  private val gameServerId: GameServerId,
  private val container: ContainerAdminHttpActionManager
) : HttpActionManager(), SmartInitializingSingleton, DisposableBean {

  override fun afterSingletonsInstantiated() {
    container.register(gameServerId.toInt(), this)
  }

  override fun destroy() {
    container.unregister(gameServerId.toInt())
  }
}
