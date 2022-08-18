package play.example.common.id

import play.SystemProps
import play.util.id.LongIdGenerator
import play.util.id.SnowflakeIdGenerator
import play.util.id.SnowflakeIdGeneratorLike
import play.util.time.Time.toMillis
import java.time.LocalDateTime

/**
 * Created by liang on 2020/6/27.
 */
class UIDGenerator private constructor(private val snowflakeIdGenerator: SnowflakeIdGenerator) : LongIdGenerator(),
  SnowflakeIdGeneratorLike by snowflakeIdGenerator {
  companion object {
    const val MAX_BITS = 63
    const val TIMESTAMP_BITS = 29
    const val NODE_ID_BITS = 24
    const val SEQUENCE_BITS = 10

    val BASE_TIMESTAMP: Long

    init {
      SystemProps.getOrDefault("play.id.baseTimestamp", "2022-01-01T00:00:00").let {
        BASE_TIMESTAMP = LocalDateTime.parse(it).toMillis() / 1000
      }
    }
  }

  constructor(serverId: Int) : this(
    SnowflakeIdGenerator(
      serverId,
      MAX_BITS,
      TIMESTAMP_BITS,
      NODE_ID_BITS,
      SEQUENCE_BITS,
      BASE_TIMESTAMP,
      SnowflakeIdGeneratorLike.ClockS
    )
  )

  override fun nextId(): Long {
    return snowflakeIdGenerator.nextId()
  }
}
