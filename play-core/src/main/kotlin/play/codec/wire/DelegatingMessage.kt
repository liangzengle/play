package play.codec.wire

import com.squareup.wire.Message

/**
 *
 * @author LiangZengle
 */
interface DelegatingMessage {
  fun getMessage(): Message<*, *>
}
