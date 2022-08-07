package play.rsocket.rpc

import io.netty.buffer.ByteBuf
import io.rsocket.Payload
import reactor.core.publisher.Flux

/**
 * @author LiangZengle
 */
interface LocalServiceCaller {

  fun serviceInterface(): Class<*>

  fun call(methodId: Int, data: ByteBuf): Any? {
    return call(methodId, data, null)
  }

  fun call(methodId: Int, data: ByteBuf, publisher: Flux<Payload>?): Any?

  object NotFound
}
