package play.example.game.app.module.platform.impl

import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.platform.PlatformService
import play.example.game.app.module.platform.domain.Platform
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
@Named
class DevPlatformService : PlatformService() {
  override val platform: Platform
    get() = Platform.Dev

  override fun validateLoginParams(params: LoginParams): Int {
    return 0
  }
}
