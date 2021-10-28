package play.example.game.app.module.server.entity

import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.NeverExpireEvaluator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@CacheSpec(
  initialSize = CacheSpec.SIZE_ONE,
  expireEvaluator = NeverExpireEvaluator::class
)
class Server(id: Int) : IntIdEntity(id) {

  private var openTime: LocalDateTime? = null

  fun getOpenDate(): Optional<LocalDate> {
    val time = openTime
    return if (time == null) Optional.empty() else Optional.of(time.toLocalDate())
  }

  fun open(time: LocalDateTime) {
    openTime = time
  }
}
