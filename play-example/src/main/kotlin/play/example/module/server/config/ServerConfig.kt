package play.example.module.server.config

import com.google.common.primitives.Bytes
import play.Configuration
import play.example.module.platform.domain.Platform
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

class ServerConfig private constructor(val serverId: Short, val platformIds: List<Byte>) {
  companion object {
    private var config: ServerConfig? = null

    val serverId: Short get() = get().serverId
    val platformId: Byte get() = get().platformIds[0]

    fun get(): ServerConfig = checkNotNull(config)

    fun init(serverId: Short, platformIds: List<Byte>) {
      check(config == null) { "ServerConfig initialized." }
      config = ServerConfig(serverId, platformIds)
    }
  }

  override fun toString(): String {
    return "ServerConfig(serverId=$serverId, platformIds=$platformIds)"
  }
}

@Singleton
class ServerConfigProvider @Inject constructor(configuration: Configuration) : Provider<ServerConfig> {

  init {
    val serverId = configuration.getInt("server.id")
    val platformNames = configuration.getStringList("server.platform")
    val platformIdArray = ByteArray(platformNames.size)
    for (i in platformNames.indices) {
      platformIdArray[i] = Platform.getOrThrow(platformNames[i]).getId()
    }
    ServerConfig.init(serverId.toShort(), Bytes.asList(*platformIdArray))
  }

  override fun get(): ServerConfig = ServerConfig.get()
}
