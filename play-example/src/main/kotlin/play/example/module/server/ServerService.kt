package play.example.module.server

import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.impl.factory.primitive.IntSets
import play.db.QueryService
import play.example.common.event.ApplicationEventBus
import play.example.module.platform.domain.Platform
import play.example.module.player.event.PlayerEventBus
import play.example.module.server.config.ServerConfig
import play.example.module.server.entity.Server
import play.example.module.server.entity.ServerCache
import play.example.module.server.event.ServerOpenEvent
import play.example.module.server.event.ServerOpenPlayerEvent
import play.util.concurrent.awaitSuccessOrThrow
import play.util.max
import play.util.primitive.toIntSaturated
import play.util.reflect.classOf
import play.util.time.betweenDays
import play.util.time.currentDate
import play.util.time.currentDateTime
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerService @Inject constructor(
  queryService: QueryService,
  val serverCache: ServerCache,
  val conf: ServerConfig,
  val playerEventBus: PlayerEventBus,
  val applicationEventBus: ApplicationEventBus
) {

  private val serverIds: ImmutableIntSet

  init {
    val idList = queryService.listIds(classOf<Server>()).awaitSuccessOrThrow(5000)
    val serverIds = IntSets.mutable.ofAll(idList)
    if (serverIds.isEmpty) {
      serverCache.create(Server(conf.serverId.toInt()))
      serverIds.add(conf.serverId.toInt())
    } else if (!serverIds.contains(conf.serverId.toInt())) {
      throw IllegalStateException("配置的服id在数据库中不存在: ${conf.serverId}")
    }
    this.serverIds = serverIds.toImmutable()
  }

  fun isServerIdValid(serverId: Int) = serverIds.contains(serverId)

  fun isPlatformNameValid(platform: Platform) = conf.platformIds.contains(platform.getId())

  fun getServerOpenDays(toDate: LocalDate = currentDate()): Int {
    val openDate = serverCache.getOrThrow(conf.serverId.toInt()).getOpenDate()
    if (openDate.isEmpty) {
      return 0
    }
    return openDate.get().betweenDays(toDate).toIntSaturated().max(0) + 1
  }

  fun open() {
    val server = serverCache.getOrThrow(conf.serverId.toInt())
    server.open(currentDateTime())
    serverCache.save(server)
    applicationEventBus.postBlocking(ServerOpenEvent)
    playerEventBus.postToOnlinePlayers { ServerOpenPlayerEvent(it) }
  }


}
