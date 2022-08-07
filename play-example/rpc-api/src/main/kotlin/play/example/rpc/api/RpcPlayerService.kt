package play.example.rpc.api

import play.rsocket.rpc.RpcServiceInterface
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
@RpcServiceInterface(generateStub = true)
interface RpcPlayerService {
  fun fireAndForget(playerId: Long, newName: String): Mono<Void>

  fun requestResponse(playerId: Long): Mono<String>

  fun requestStream(playerId: Long): Flux<String>

  fun requestChannel(playerId: Long, flux: Flux<Int>): Flux<String>
}
