package play.example.game.app.module.platform.impl

import org.springframework.stereotype.Component
import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.platform.PlatformService
import play.example.game.app.module.platform.domain.Platform

/**
 * Created by liang on 2020/6/27.
 */
@Component
class DevPlatformService : PlatformService() {
  override val platform: Platform
    get() = Platform.Dev

  override fun validateLoginParams(params: LoginParams): Int {
    return 0
  }
}
