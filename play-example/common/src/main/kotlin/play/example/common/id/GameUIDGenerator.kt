package play.example.common.id

import java.util.*
import java.util.concurrent.atomic.AtomicLong
import play.util.forEach
import play.util.primitive.toInt

/**
 * Created by liang on 2020/6/27.
 */
class GameUIDGenerator(
  private val bitsMax: Int,
  private val platformBits: Int,
  private val serverBits: Int,
  private val platformId: Int,
  private val serverId: Int,
  currentId: OptionalLong
) : LongIdGenerator() {
  val min: Long
  val max: Long
  private val current: AtomicLong

  init {
    require(bitsMax in 1..63) { "`bitsMax` illegal: $bitsMax" }
    require(platformBits in 1..31) { "`platformBits` illegal: $platformBits" }
    require(serverBits in 1..31) { "`serverBits` illegal: $serverBits" }
    require(platformId > 0 && platformId < (1 shl platformBits)) { "`platformValue` overflow: $platformId" }
    require(serverId > 0 && serverId < (1 shl serverBits)) { "`serverValue` overflow: $serverId" }

    min = platformId.toLong() shl (bitsMax - platformBits) or
      (serverId.toLong() shl (bitsMax - platformBits - serverBits))
    max = min or (1L shl bitsMax - platformBits - serverBits) - 1
    currentId.forEach { current -> require(current in min..max) { "`currentId` overflow: $current" } }
    current = AtomicLong(currentId.orElse(min - 1))
  }

  constructor(
    platformId: Int,
    serverId: Int,
    currentId: OptionalLong
  ) : this(BitsMax, PlatformIdBits, ServerIdBits, platformId, serverId, currentId)

  override fun nextOrThrow(): Long {
    val nextId = next0()
    if (nextId == -1L) {
      throw IdExhaustedException(min, max, current.get())
    }
    return nextId
  }

  override fun next(): OptionalLong {
    val nextId = next0()
    return if (nextId == -1L) OptionalLong.empty() else OptionalLong.of(nextId)
  }

  override fun hasNext(): Boolean {
    return current.get() < max
  }

  private fun next0(): Long {
    var prevId: Long
    var nextId: Long
    do {
      prevId = current.get()
      if (prevId == max) {
        nextId = -1
        break
      }
      nextId = prevId + 1
    } while (!current.compareAndSet(prevId, nextId))
    return nextId
  }

  override fun toString(): String {
    return "GameUIDGenerator(min=$min, max=$max, current=$current)"
  }

  companion object {
    // id最大位数
    private const val BitsMax = 53

    // 平台id位数
    private const val PlatformIdBits = 7

    // 服id位数
    private const val ServerIdBits = 15

    private const val PlatformIdMask = (1.toLong() shl PlatformIdBits) - 1
    private const val ServerIdMask = (1.toLong() shl ServerIdBits) - 1

    fun fromId(id: Long): GameUIDGenerator {
      val platformId = getPlatformId(id)
      val serverId = getServerId(id)
      return GameUIDGenerator(platformId.toInt(), serverId.toInt(), OptionalLong.of(id))
    }

    fun getPlatformId(id: Long): Byte = (id shr (BitsMax - PlatformIdBits) and PlatformIdMask).toByte()
    fun getServerId(id: Long): Short = (id shr (BitsMax - PlatformIdBits - ServerIdBits) and ServerIdMask).toShort()
    fun getServerUid(id: Long): Int = toInt(getPlatformId(id).toShort(), getServerId(id))
  }
}
