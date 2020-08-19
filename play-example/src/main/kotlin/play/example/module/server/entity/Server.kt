package play.example.module.server.entity

import play.db.EntityInt
import play.db.cache.CacheSpec
import play.db.cache.NeverExpireEvaluator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


@CacheSpec(
  initialSize = CacheSpec.SIZE_ONE,
  persistType = CacheSpec.PersistType.Manually,
  expireEvaluator = NeverExpireEvaluator::class
)
class Server(id: Int) : EntityInt(id) {

  private var openTime: LocalDateTime? = null

  fun getOpenDate(): Optional<LocalDate> {
    val time = openTime
    return if (time == null) Optional.empty() else Optional.of(time.toLocalDate())
  }

  fun open(time: LocalDateTime) {
    openTime = time
  }
}
