package play.db.mongo.codec

import org.bson.io.BasicOutputBuffer

/**
 *
 * @author LiangZengle
 */
internal class ReusableOutputBuffer : BasicOutputBuffer(256) {

  override fun close() {
    truncateToPosition(0)
  }
}
