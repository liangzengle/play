package play.example.game.app.module.server.entity

import play.db.Merge
import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.ImmutableEntity
import play.entity.cache.InitialCacheSize
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Merge(Merge.Strategy.All)
@InitialCacheSize(InitialCacheSize.ONE)
@CacheSpec(neverExpire = true)
@ImmutableEntity
class ServerEntity(id: Int) : IntIdEntity(id) {

  private var openTime: LocalDateTime? = null
  private var mergeTime: LocalDateTime? = null

  fun getOpenDate(): Optional<LocalDate> {
    val time = openTime
    return if (time == null) Optional.empty() else Optional.of(time.toLocalDate())
  }

  fun getOpenTime(): LocalDateTime? = openTime

  fun getMerTime(): LocalDateTime? = mergeTime

  fun open(time: LocalDateTime) {
    openTime = time
  }
}
