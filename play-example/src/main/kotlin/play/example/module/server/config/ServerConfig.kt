package play.example.module.server.config

import play.Configuration
import play.example.module.platform.domain.Platform
import play.util.collection.toImmutableSet
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

data class ServerConfig(val serverId: Short, val platformIds: Set<Byte>)

@Singleton
class ServerConfigProvider @Inject constructor(configuration: Configuration) : Provider<ServerConfig> {
  private val conf: ServerConfig

  init {
    val serverId = configuration.getInt("server.id")
    val platformNames = configuration.getStringList("server.platform")
    val platformIds = platformNames.asSequence().map { Platform.getOrThrow(it).getId() }.toImmutableSet()
    conf = ServerConfig(serverId.toShort(), platformIds)
  }

  override fun get(): ServerConfig = conf
}
