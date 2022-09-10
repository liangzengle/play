package play.example.rpc.test

import org.springframework.stereotype.Component
import play.example.rpc.api.RpcPlayerService
import play.rsocket.rpc.RpcServiceImplementation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
@Component
@RpcServiceImplementation(RpcPlayerService::class)
class RpcServiceImpl : RpcPlayerService {
  override fun fireAndForget(playerId: Long, newName: String): Mono<Void> {
    return Mono.empty()
  }

  override fun requestResponse(playerId: Long): Mono<String> {
    return Mono.just(playerId.toString())
  }

  override fun requestStream(playerId: Long): Flux<String> {
    return Flux.interval(Duration.ofSeconds(2)).map { it.toString() }
  }

  override fun requestChannel(playerId: Long, flux: Flux<Int>): Flux<String> {
    return Flux.interval(Duration.ofSeconds(2)).map { it.toString() }
  }
}
