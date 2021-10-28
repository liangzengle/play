package play.example.game.app.module.platform

import org.springframework.stereotype.Component
import play.example.game.app.module.platform.domain.Platform
import play.util.collection.toImmutableEnumMap

/**
 * Created by liang on 2020/6/27.
 */
@Component
class PlatformServiceProvider(serviceList: List<PlatformService>) {

  private val services = serviceList.toImmutableEnumMap { it.platform }

  init {
    if (serviceList.size != Platform.size) {
      val platforms = Platform.list().asSequence().filterNot(services::containsKey).toList()
      throw IllegalStateException("以下平台缺少PlatformService: $platforms")
    }
  }

  fun getService(platform: Platform) = services[platform] ?: error("should not happen")

  fun getService(platformId: Int) = getService(Platform.getOrThrow(platformId))

  fun getService(platformName: String) = getService(Platform.getOrThrow(platformName))

  fun getServiceOrNull(platformName: String): PlatformService? {
    return Platform.getOrNull(platformName)?.let { getService(it) }
  }

  fun getServiceOrNull(platform: Platform): PlatformService? = services[platform]
}
