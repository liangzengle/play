package play.example.robot.bt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.account.message.LoginParams
import play.example.robot.module.AccountModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class DummyBehaviorTree {

  @Autowired
  private lateinit var accountModule: AccountModule

  fun run(player: RobotPlayer) {
    val params = LoginParams("Dev", 1, player.account)
    accountModule.loginReq(player, params)
  }

}
