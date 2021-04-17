package play.example.game.module.server.config

import com.google.common.primitives.Bytes
import com.typesafe.config.Config
import java.util.*
import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import play.example.game.module.platform.domain.Platform
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.example.common.id.GameUIDGenerator

class ServerConfig private constructor(val serverId: Short, val platformIds: List<Byte>) {
  companion object {
    private lateinit var config: ServerConfig

    lateinit var serverIds: ImmutableIntSet
      private set

    val serverId: Short get() = config.serverId
    val platformId: Byte get() = config.platformIds[0]

    fun get(): ServerConfig = config

    @Synchronized
    fun init(serverId: Short, platformIds: List<Byte>) {
      if (this::config.isInitialized) {
        throw IllegalStateException("config has been initialized.")
      }
      config = ServerConfig(serverId, platformIds)
    }

    @Synchronized
    fun initServerIds(serverIds: ImmutableIntSet) {
      if (this::serverIds.isInitialized) {
        throw IllegalStateException("serverIds has been initialized.")
      }
      this.serverIds = serverIds
    }

    @JvmStatic
    fun newIdGenerator(): GameUIDGenerator =
      GameUIDGenerator(platformId.toInt(), serverId.toInt(), OptionalLong.empty())
  }

  override fun toString(): String {
    return "ServerConfig(serverId=$serverId, platformIds=$platformIds)"
  }
}

@Singleton
class ServerConfigProvider @Inject constructor(conf: Config) : Provider<ServerConfig> {

  init {
    val serverId = conf.getInt("server.id")
    val platformNames = conf.getStringList("server.platform")
    val platformIdArray = ByteArray(platformNames.size)
    for (i in platformNames.indices) {
      platformIdArray[i] = Platform.getOrThrow(platformNames[i]).getId()
    }
    ServerConfig.init(serverId.toShort(), Bytes.asList(*platformIdArray))
  }

  override fun get(): ServerConfig = ServerConfig.get()
}
