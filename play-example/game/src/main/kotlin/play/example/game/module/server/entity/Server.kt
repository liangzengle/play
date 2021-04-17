package play.example.game.module.server.entity

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import play.entity.EntityInt
import play.entity.cache.CacheSpec
import play.entity.cache.NeverExpireEvaluator

@CacheSpec(
  initialSize = CacheSpec.SIZE_ONE,
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
