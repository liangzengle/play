package play.example.game.module.platform.impl

import javax.inject.Singleton
import play.example.game.module.account.message.LoginParams
import play.example.game.module.platform.PlatformService
import play.example.game.module.platform.domain.Platform

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
class DevPlatformService : PlatformService() {
  override val platform: Platform
    get() = Platform.Dev

  override fun validateLoginParams(params: LoginParams): Int {
    return 0
  }
}
