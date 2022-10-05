package play.example.game.app.module.activity.base.res

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import org.eclipse.collections.api.factory.primitive.IntSets
import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.api.set.primitive.MutableIntSet
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.trigger.ActivityTimeTrigger
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.server.condition.ServerCondition
import play.res.AbstractResource
import play.res.ExtensionKey
import play.res.ResourceExtension
import play.res.validation.constraints.ReferTo
import java.time.Duration
import java.util.*

/**
 *
 * @author LiangZengle
 */
class ActivityResource(
  /**
   * 活动类型
   */
  val type: ActivityType,
  /**
   * 开始时间
   */
  val startTime: ActivityTimeTrigger,
  /**
   * 开启时长
   */
  val duration: Duration
) : AbstractResource(), ExtensionKey<ActivityResourceExtension>, Comparable<ActivityResource> {

  /**
   * 父活动id
   */
  @ReferTo(ActivityResource::class)
  val parentId: Int = 0

  /**
   * 预告提前时间
   */
  val noticeAhead: Duration = Duration.ZERO

  /**
   * 关闭延迟
   */
  val closeDelay: Duration = Duration.ZERO

  /**
   * 活动期间定时触发事件: {"eventName": {"cron": "0 0 * * * ?"},}
   */
  val eventTriggers = emptyMap<String, ActivityTimeTrigger>()

  /**
   * 版本号
   */
  val version = 0

  /**
   * 活动初始化条件，不符合条件的活动会被忽略
   */
  val initConditions = emptyList<ServerCondition>()

  /**
   * 开启条件，仅在进入开启阶段前判断1次
   */
  val startConditions = emptyList<ServerCondition>()

  /**
   * 玩家参与条件
   */
  val joinConditions = emptyList<PlayerCondition>()

  /**
   * 开启次数
   */
  val openTimes = 0

  override fun compareTo(other: ActivityResource): Int {
    return if (this.parentId == other.id) -1 else this.id.compareTo(other.id)
  }
}

class ActivityResourceExtension(list: List<ActivityResource>) : ResourceExtension<ActivityResource>(list) {
  private val childActivities: IntObjectMap<MutableIntSet>

  private val typeActivities: Map<ActivityType, IntSet>

  init {
    val childMap = IntObjectMaps.mutable.empty<MutableIntSet>()
    val typeMap = EnumMap<ActivityType, MutableIntSet>(ActivityType::class.java)
    for (resource in list) {
      if (resource.parentId > 0) {
        childMap.getIfAbsentPut(resource.parentId) { IntSets.mutable.empty() }.add(resource.id)
      }
      typeMap.computeIfAbsent(resource.type) { IntSets.mutable.empty() }.add(resource.id)
    }
    childActivities = childMap
    typeActivities = typeMap
  }

  fun getChildActivityIds(parentId: Int): IntSet = childActivities.get(parentId) ?: IntSets.immutable.empty()

  fun getChildActivityResources(parentId: Int): Iterable<ActivityResource> {
    return getChildActivityIds(parentId).asLazy().collect { ActivityResourceSet.getOrThrow(it) }
  }

  fun getActivityIds(activityType: ActivityType): IntSet = typeActivities[activityType] ?: IntSets.immutable.empty()

  fun getActivityResources(activityType: ActivityType): Iterable<ActivityResource> =
    getActivityIds(activityType).asLazy().collect { ActivityResourceSet.getOrThrow(it) }
}
