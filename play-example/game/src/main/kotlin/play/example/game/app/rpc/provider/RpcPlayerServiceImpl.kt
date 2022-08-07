package play.example.game.app.rpc.provider

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import play.example.game.app.module.player.PlayerService
import play.example.game.container.gs.domain.GameServerId
import play.example.rpc.api.RpcPlayerService
import play.rsocket.rpc.RpcServiceImplementation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * @author LiangZengle
 */
@Service
@RpcServiceImplementation(RpcPlayerService::class)
class RpcPlayerServiceImpl(private val playerService: PlayerService, private val gameServerId: GameServerId) :
  RpcPlayerService {

  private val logger = LoggerFactory.getLogger(RpcPlayerServiceImpl::class.java)

  private val requestStreamCounter = AtomicInteger()

  override fun requestResponse(playerId: Long): Mono<String> {
    logger.info("RpcPlayerServiceImpl.requestResponse: $playerId")
    return Mono.just(playerService.getPlayerNameOrElse(playerId, "$playerId @ $gameServerId"))
  }

  override fun fireAndForget(playerId: Long, newName: String): Mono<Void> {
    logger.info("RpcPlayerServiceImpl.fireAndForget: $playerId, $newName")
    return Mono.empty()
  }

  override fun requestStream(playerId: Long): Flux<String> {
    logger.info("RpcPlayerServiceImpl.requestStream: $playerId")
    return Flux.interval(Duration.ofSeconds(2)).map { requestStreamCounter.incrementAndGet().toString() }
  }

  override fun requestChannel(playerId: Long, flux: Flux<Int>): Flux<String> {
    logger.info("RpcPlayerServiceImpl.requestChannel: $playerId")

    return Flux.from(flux.doOnNext {
      logger.info("RpcPlayerServiceImpl.requestChannel receive: $it")
    }).map { it.toString() }
  }
}
