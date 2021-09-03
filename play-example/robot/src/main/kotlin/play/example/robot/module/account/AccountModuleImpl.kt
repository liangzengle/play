package play.example.robot.module.account

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.robot.module.AccountModule
import play.example.robot.module.PlayerModule
import play.example.robot.module.player.RobotPlayer
import play.util.rd

/**
 *
 * @author LiangZengle
 */
@Component
class AccountModuleImpl @Autowired constructor(private val playerModule: PlayerModule) : AccountModule() {
  override fun pingResp(player: RobotPlayer, data: String, req: PingRequestParams?) {
    playerModule.pingResp(player, data, req?.let { PlayerModule.PingRequestParams(it.msg) })
    pingReq(player, "hello")
  }

  override fun loginResp(player: RobotPlayer, data: Boolean, req: LoginRequestParams?) {
    if (data) {
      playerModule.loginReq(player)
    } else {
      playerModule.createReq(player, randomName())
    }
  }

  private fun randomName(): String {
    val b = StringBuilder(10)
    for (i in 1..10) {
      val c = rd.nextInt('a'.code, 'z'.code).toChar()
      b.append(c)
    }
    return b.toString()
  }
}
