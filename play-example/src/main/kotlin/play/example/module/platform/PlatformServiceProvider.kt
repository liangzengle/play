package play.example.module.platform

import play.example.module.platform.domain.Platform
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableEnumMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
class PlatformServiceProvider @Inject constructor(val injector: Injector) : PostConstruct {

  private lateinit var services: Map<Platform, PlatformService>
  override fun postConstruct() {
    services = injector.instancesOf(PlatformService::class.java).toImmutableEnumMap { it.platform }
    if (services.size != Platform.size()) {
      val platforms = Platform.elems.asSequence().filterNot(services::containsKey).toList()
      throw IllegalStateException("以下平台缺少PlatformService: $platforms")
    }
  }

  fun getService(platform: Platform) = services[platform] ?: error("should not happen")

  fun getService(platformId: Int) = getService(Platform.getOrThrow(platformId))

  fun getService(platformName: String) = getService(Platform.getOrThrow(platformName))

  fun getServiceOrNull(platformName: String): PlatformService? {
    return Platform.getOrNull(platformName)?.let { getService(it) }
  }
}
