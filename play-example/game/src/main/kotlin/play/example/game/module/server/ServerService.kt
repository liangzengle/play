package play.example.game.module.server

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.seconds
import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.impl.factory.primitive.IntSets
import play.ApplicationEventBus
import play.db.QueryService
import play.example.game.module.platform.domain.Platform
import play.example.game.module.player.event.PlayerEventBus
import play.example.game.module.server.config.ServerConfig
import play.example.game.module.server.entity.Server
import play.example.game.module.server.entity.ServerEntityCache
import play.example.game.module.server.event.ServerOpenEvent
import play.example.game.module.server.event.ServerOpenPlayerEvent
import play.util.classOf
import play.util.max
import play.util.primitive.toIntSaturated
import play.util.time.betweenDays
import play.util.time.currentDate
import play.util.time.currentDateTime

@Singleton
class ServerService @Inject constructor(
  queryService: QueryService,
  val serverCache: ServerEntityCache,
  val conf: ServerConfig,
  val playerEventBus: PlayerEventBus,
  val applicationEventBus: ApplicationEventBus
) {

  private val serverIds: ImmutableIntSet

  init {
    val idList = queryService.listIds(classOf<Server>()).get(5.seconds)
    val serverIds = IntSets.mutable.ofAll(idList.stream().mapToInt(Int::toInt))
    if (serverIds.isEmpty) {
      serverCache.create(Server(conf.serverId.toInt()))
      serverIds.add(conf.serverId.toInt())
    } else if (!serverIds.contains(conf.serverId.toInt())) {
      throw IllegalStateException("配置的服id在数据库中不存在: ${conf.serverId}")
    }
    this.serverIds = serverIds.toImmutable()
    ServerConfig.initServerIds(this.serverIds)
  }

  fun isServerIdValid(serverId: Int) = serverIds.contains(serverId)

  fun isPlatformValid(platform: Platform) = conf.platformIds.contains(platform.getId())

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
    applicationEventBus.postBlocking(ServerOpenEvent)
    playerEventBus.postToOnlinePlayers { ServerOpenPlayerEvent(it) }
  }
}
