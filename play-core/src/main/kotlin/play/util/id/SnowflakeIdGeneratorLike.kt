package play.util.id

import play.time.Time

interface SnowflakeIdGeneratorLike {

  fun nextId(): Long

  fun extractTimestamp(id: Long): Long

  fun extractNodeId(id: Long): Long

  fun extractSequence(id: Long): Long

  interface Clock {
    fun time(): Long
  }

  object ClockMS : Clock {
    override fun time(): Long {
      return Time.currentMillis()
    }
  }

  object ClockS : Clock {
    override fun time(): Long {
      return Time.currentSeconds()
    }
  }
}
