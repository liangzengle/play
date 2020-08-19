package play.example.module.platform.impl

import play.example.module.platform.PlatformService
import play.example.module.platform.domain.Platform
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
class DevPlatformService : PlatformService() {
  override val platform: Platform
    get() = Platform.Dev
}
