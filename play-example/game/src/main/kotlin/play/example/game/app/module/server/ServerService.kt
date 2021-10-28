package play.example.game.app.module.server

import com.google.common.eventbus.Subscribe
import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.impl.factory.primitive.IntSets
import org.springframework.stereotype.Component
import play.db.QueryService
import play.event.EventBus
import play.example.game.app.module.platform.domain.Platform
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.server.entity.Server
import play.example.game.app.module.server.entity.ServerEntityCache
import play.example.game.app.module.server.event.ServerOpenEvent
import play.example.game.app.module.server.event.ServerOpenPlayerEvent
import play.example.game.app.module.server.res.ServerConfig
import play.inject.SpringPlayInjector
import play.util.classOf
import play.util.max
import play.util.primitive.toIntSaturated
import play.util.time.Time.betweenDays
import play.util.time.Time.currentDate
import play.util.time.Time.currentDateTime
import java.time.LocalDate
import kotlin.time.seconds

@Component
class ServerService(
  queryService: QueryService,
  private val serverCache: ServerEntityCache,
  private val conf: ServerConfig,
  private val playerEventBus: PlayerEventBus,
  private val eventBus: EventBus,
  injector: SpringPlayInjector
) {

  private val serverIds: ImmutableIntSet

  init {
    val idList = queryService.listIds(classOf<Server>()).get(5.seconds)
    val serverIds = IntSets.mutable.ofAll(idList.stream().mapToInt(Int::toInt))
    if (serverIds.isEmpty) {
      injector.whenAvailable {
        serverCache.create(Server(conf.serverId.toInt()))
      }
      serverIds.add(conf.serverId.toInt())
    } else if (!serverIds.contains(conf.serverId.toInt())) {
      throw IllegalStateException("配置的服id在数据库中不存在: ${conf.serverId}")
    }
    this.serverIds = serverIds.toImmutable()
  }

  fun getServerIds(): IntSet {
    return serverIds
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
    // TODO post event
    eventBus.postSync(ServerOpenEvent)
    playerEventBus.postToOnlinePlayers { ServerOpenPlayerEvent(it) }
  }

  @Subscribe
  private fun onServerOpen(event: ServerOpenEvent) {
    println("test event receive: $event")
  }
}
