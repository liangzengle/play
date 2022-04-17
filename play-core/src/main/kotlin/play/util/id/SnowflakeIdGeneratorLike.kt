package play.util.id

interface SnowflakeIdGeneratorLike {

  fun nextId(): Long

  fun extractTimestamp(id: Long): Long

  fun extractNodeId(id: Long): Long

  fun extractSequence(id: Long): Long

  interface Clock {
    fun time(): Long
  }

  object SystemClockMS : Clock {
    override fun time(): Long {
      return System.currentTimeMillis()
    }
  }

  object SystemClockS : Clock {
    override fun time(): Long {
      return System.currentTimeMillis() / 1000
    }
  }
}
