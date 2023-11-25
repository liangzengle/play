package play.util.id


interface SnowflakeIdGeneratorLike {

  fun nextId(): Long

  fun extractTimestamp(id: Long): Long

  fun extractNodeId(id: Long): Long

  fun extractSequence(id: Long): Long
}
