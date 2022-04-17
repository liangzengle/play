package play.util.id

import java.util.concurrent.atomic.AtomicLongFieldUpdater

class SnowflakeIdGenerator(
  nodeId: Int,
  maxBits: Int,
  timestampBits: Int,
  nodeIdBits: Int,
  private val sequenceBits: Int,
  private val timestampOffset: Long,
  private val clock: SnowflakeIdGeneratorLike.Clock
) : LongIdGenerator(), SnowflakeIdGeneratorLike {
  companion object {
    private val SequenceUpdater =
      AtomicLongFieldUpdater.newUpdater(SnowflakeIdGenerator::class.java, "timestampSequence")
    const val MAX_BITS_DEFAULT = 63
    const val TIMESTAMP_BITS_DEFAULT = 41
    const val NODE_ID_BITS_DEFAULT = 10
    const val SEQUENCE_BITS_DEFAULT = 12
  }

  constructor(nodeId: Int) : this(
    nodeId,
    MAX_BITS_DEFAULT,
    TIMESTAMP_BITS_DEFAULT,
    NODE_ID_BITS_DEFAULT,
    SEQUENCE_BITS_DEFAULT,
    0,
    SnowflakeIdGeneratorLike.SystemClockMS
  )

  private val nodeIdAndSequenceBits = nodeIdBits + sequenceBits
  private val nodeBits = nodeId.toLong() shl sequenceBits
  private val maxSequence = (1L shl sequenceBits) - 1
  private val maxNodeId = (1L shl nodeIdBits) - 1
  private val maxTimestamp = (1L shl timestampBits) - 1

  init {
    require(timestampBits > 0) { "epochBits must be greater than 0" }
    require(nodeIdBits > 0) { "nodeIdBits must be greater than 0" }
    require(sequenceBits > 0) { "sequenceBits must be greater than 0" }
    require((timestampBits + nodeIdBits + sequenceBits) <= maxBits) {
      "epochBits + nodeIdBits + sequenceBits must be less than or equal to maxBits"
    }
    require(maxBits < 64) { "maxBits must be less than 64" }
    require(nodeId in 1..maxNodeId) { "nodeId must be in range [1, $maxNodeId]" }
    require(clock.time() >= timestampOffset) {
      "clock.millis() must be greater than or equal to timestampOffsetMs"
    }
    val epoch = clock.time() - timestampOffset
    require(epoch <= maxTimestamp) { "clock.millis() - timestampOffsetMs must be less than or equal to maxTimestamp" }
  }

  @Volatile
  private var timestampSequence = 0L

  override fun nextId(): Long {
    while (true) {
      val oldTimestampSequence = timestampSequence
      val timestamp = clock.time() - timestampOffset
      if (timestamp > maxTimestamp) {
        throw IllegalStateException("timestampMs must be less than maxTimestamp")
      }
      val oldTimestamp = oldTimestampSequence ushr nodeIdAndSequenceBits
      if (timestamp > oldTimestamp) {
        val newTimestampSequence = timestamp shl nodeIdAndSequenceBits
        if (SequenceUpdater.compareAndSet(this, oldTimestampSequence, newTimestampSequence)) {
          return newTimestampSequence or nodeBits
        }
      } else if (timestamp == oldTimestamp) {
        val oldSequence = oldTimestampSequence and maxSequence
        if (oldSequence < maxSequence) {
          val newTimestampSequence = oldTimestampSequence + 1
          if (SequenceUpdater.compareAndSet(this, oldTimestampSequence, newTimestampSequence)) {
            return newTimestampSequence or nodeBits
          }
        }
      } else {
        throw IllegalStateException(
          "clock has gone backwards: timestampMs=$timestamp < oldTimestampMs=$oldTimestamp"
        )
      }
      check(!Thread.currentThread().isInterrupted) { "unexpected thread interrupt" }
      Thread.onSpinWait()
    }
  }

  override fun extractTimestamp(id: Long): Long {
    return id ushr nodeIdAndSequenceBits
  }

  override fun extractNodeId(id: Long): Long {
    return (id ushr sequenceBits) and maxNodeId
  }

  override fun extractSequence(id: Long): Long {
    return id and maxSequence
  }
}
