package play.example.common.id

import play.util.collection.forEach
import play.util.primitive.toInt
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by liang on 2020/6/27.
 */
@Suppress("ConvertTwoComparisonsToRangeCheck")
class GameUIDGenerator(
  val bitsMax: Int,
  val platformBits: Int,
  val serverBits: Int,
  val platformValue: Int,
  val serverValue: Int,
  currentId: OptionalLong
) : LongIdGenerator() {
  val min: Long
  val max: Long
  private val current: AtomicLong

  init {
    require(bitsMax > 0 && bitsMax < 64) { "`bitsMax` illegal: $bitsMax" }
    require(platformBits > 0 && platformBits < 32) { "`platformBits` illegal: $platformBits" }
    require(serverBits > 0 && serverBits < 32) { "`serverBits` illegal: $serverBits" }
    require(platformValue > 0 && platformValue < (1 shl platformBits)) { "`platformValue` overflow: $platformValue" }
    require(serverValue > 0 && serverValue < (1 shl serverBits)) { "`serverValue` overflow: $serverValue" }

    min = platformValue.toLong() shl (bitsMax - platformBits) or
      (serverValue.toLong() shl (bitsMax - platformBits - serverBits))
    max = min or (1L shl bitsMax - platformBits - serverBits) - 1
    currentId.forEach { current -> require(current >= min && current <= max) { "`currentId` overflow: $current" } }
    current = AtomicLong(currentId.orElse(min - 1))
  }

  constructor(
    platformValue: Int,
    serverValue: Int,
    currentId: OptionalLong
  ) : this(BitsMax, PlatformIdBits, ServerIdBits, platformValue, serverValue, currentId)

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
    private const val BitsMax = 53
    private const val PlatformIdBits = 7
    private const val ServerIdBits = 15
    private const val PlatformIdMask = (1.toLong() shl PlatformIdBits) - 1
    private const val ServerIdMask = (1.toLong() shl ServerIdBits) - 1

    fun getPlatformId(id: Long): Byte = (id shr (BitsMax - PlatformIdBits) and PlatformIdMask).toByte()
    fun getServerId(id: Long): Short = (id shr (BitsMax - PlatformIdBits - ServerIdBits) and ServerIdMask).toShort()
    fun getServerUid(id: Long): Int = toInt(getPlatformId(id).toShort(), getServerId(id))
  }
}
