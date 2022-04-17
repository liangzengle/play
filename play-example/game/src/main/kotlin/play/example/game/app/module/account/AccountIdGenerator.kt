package play.example.game.app.module.account

import play.example.common.id.UIDGenerator
import play.util.id.LongIdGenerator
import play.util.primitive.toIntChecked
import play.util.rd
import java.util.concurrent.atomic.AtomicLongFieldUpdater

class AccountIdGenerator(
  private val nodeId: Int,
  private val nodeBits: Int,
  private val rdBits: Int,
  private val sequenceOffset: Int
) : LongIdGenerator() {

  companion object {
    private const val NODE_ID_BITS = UIDGenerator.NODE_ID_BITS
    private const val RD_BITS = 3

    private const val MAX_NODE_ID = (1 shl NODE_ID_BITS) - 1

    fun extractSequence(id: Long): Int {
      return (id ushr (NODE_ID_BITS + RD_BITS)).toIntChecked()
    }

    fun extractNodeId(id: Long): Int {
      return ((id ushr RD_BITS) and MAX_NODE_ID.toLong()).toIntChecked()
    }

    private val SequenceUpdater = AtomicLongFieldUpdater.newUpdater(AccountIdGenerator::class.java, "sequence")
  }

  constructor(nodeId: Int, sequenceOffset: Int) : this(nodeId, NODE_ID_BITS, RD_BITS, sequenceOffset)

  private val maxNodeId = (1 shl nodeBits) - 1
  private val maxRd = (1 shl rdBits) - 1
  private val sequenceShift = nodeBits + rdBits

  @Volatile
  private var sequence = sequenceOffset.toLong()

  override fun nextId(): Long {
    val nextSequence = SequenceUpdater.incrementAndGet(this)
    val rd = rd.nextInt(0, maxRd + 1).toLong()
    return (nextSequence shl sequenceShift) or (nodeId.toLong() shl rdBits) or rd
  }

  fun extractSequence(id: Long): Int {
    return (id ushr (nodeBits + rdBits)).toIntChecked()
  }

  fun extractNodeId(id: Long): Int {
    return ((id ushr rdBits) and maxNodeId.toLong()).toIntChecked()
  }
}
