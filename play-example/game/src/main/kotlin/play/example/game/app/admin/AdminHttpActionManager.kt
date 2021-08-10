package play.example.game.app.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.container.admin.ContainerAdminHttpActionManager
import play.example.game.container.gs.domain.GameServerId
import play.net.http.HttpActionManager

@Component
class AdminHttpActionManager @Autowired constructor(
  gameServerId: GameServerId,
  container: ContainerAdminHttpActionManager
) : HttpActionManager() {

  init {
    @Suppress("LeakingThis")
    container.registerActionManager(gameServerId.toInt(), this)
  }
}
