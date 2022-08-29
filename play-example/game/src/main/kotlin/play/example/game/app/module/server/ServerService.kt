package play.example.game.app.module.server

import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.api.set.primitive.MutableIntSet
import org.eclipse.collections.impl.factory.primitive.IntSets
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import play.db.QueryService
import play.entity.cache.EntityCacheWriter
import play.event.EventBus
import play.event.EventListener
import play.event.EventReceive
import play.example.game.app.module.platform.domain.Platform
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.server.entity.ServerEntity
import play.example.game.app.module.server.entity.ServerEntityCache
import play.example.game.app.module.server.event.ServerOpenEvent
import play.example.game.app.module.server.event.ServerOpenPlayerEvent
import play.example.game.app.module.server.res.ServerConfig
import play.spring.OrderedSmartInitializingSingleton
import play.util.classOf
import play.util.max
import play.util.primitive.toIntSaturated
import play.util.time.Time.betweenDays
import play.util.time.Time.currentDate
import play.util.time.Time.currentDateTime
import java.time.Duration
import java.time.LocalDate

@Component
class ServerService(
  queryService: QueryService,
  private val serverEntityCache: ServerEntityCache,
  private val conf: ServerConfig,
  private val playerEventBus: PlayerEventBus,
  private val eventBus: EventBus,
  private val entityCacheWriter: EntityCacheWriter
) : OrderedSmartInitializingSingleton, EventListener {

  private val serverIds: ImmutableIntSet

  private val serverId = conf.serverId.toInt()

  init {
    val serverIds: MutableIntSet = queryService.queryIds(classOf<ServerEntity>())
      .collect({ IntSets.mutable.empty() }, { set, id -> set.apply { add(id) } })
      .block(Duration.ofSeconds(5))!!
    if (serverIds.isEmpty) {
      serverIds.add(serverId)
    } else if (!serverIds.contains(serverId)) {
      throw IllegalStateException("配置的服id在数据库中不存在: $serverId")
    }
    this.serverIds = serverIds.toImmutable()
  }

  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    serverEntityCache.getOrCreate(serverId, ::ServerEntity)
  }

  override fun eventReceive(): EventReceive {
    return newEventReceiveBuilder()
      .match(::onServerOpen)
      .build()
  }

  fun getServerIds(): IntSet {
    return serverIds
  }

  fun isServerIdValid(serverId: Int) = serverIds.contains(serverId)

  fun isPlatformValid(platform: Platform) = conf.platformIds.contains(platform.id().toByte())

  fun getServerOpenDays(toDate: LocalDate = currentDate()): Int {
    val openDate = serverEntityCache.getOrThrow(serverId).getOpenDate()
    if (openDate.isEmpty) {
      return 0
    }
    return openDate.get().betweenDays(toDate).toIntSaturated().max(0) + 1
  }

  fun tryOpen() {
    val entity = serverEntityCache.getOrThrow(serverId)
    if (entity.getOpenDate().isPresent) {
      return
    }
    synchronized(entity) {
      if (entity.getOpenDate().isEmpty) {
        entity.open(currentDateTime())
      }
    }
    entityCacheWriter.update(entity)
    eventBus.post(ServerOpenEvent)
    playerEventBus.postToOnlinePlayers { ServerOpenPlayerEvent(it) }
  }

  private fun onServerOpen(event: ServerOpenEvent) {
    println("test event receive: $event")
  }
}
