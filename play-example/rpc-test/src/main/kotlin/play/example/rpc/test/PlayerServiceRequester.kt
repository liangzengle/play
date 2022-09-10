package play.example.rpc.test

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import play.example.rpc.api.RpcPlayerService
import play.rsocket.client.event.RSocketClientInitializedEvent
import play.rsocket.rpc.RpcClient
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerServiceRequester(private val rpcClient: RpcClient) : ApplicationListener<RSocketClientInitializedEvent> {
  private val logger = LoggerFactory.getLogger(PlayerServiceRequester::class.java)

  private val counter = AtomicInteger()
  private val invokeCounter = AtomicInteger()

  override fun onApplicationEvent(event: RSocketClientInitializedEvent) {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val executor = Executors.newSingleThreadExecutor()
    scheduler.scheduleWithFixedDelay({ executor.execute(::fireAndForget) }, 10, 2, TimeUnit.SECONDS)
    scheduler.scheduleWithFixedDelay({ executor.execute(::requestResponse) }, 10, 2, TimeUnit.SECONDS)

    requestStream()
    requestChannel()
  }

  fun fireAndForget() {
    val rpcService = rpcClient.getRpcService(RpcPlayerService::class.java, 1)
    rpcService.fireAndForget(8936832658046976, "name")

    val rpcService2 = rpcClient.getRpcService(RpcPlayerService::class.java, 100)
    rpcService2.fireAndForget(8936832658046976, "name")
  }

  fun requestResponse() {
    val rpcService = rpcClient.getRpcService(RpcPlayerService::class.java, 1)
    rpcService.requestResponse(8936832658046976).subscribe({ playerName ->
      logger.info("${LocalDateTime.now()}     requestResponse: $playerName")
    }, { e ->
      e.printStackTrace()
    })
  }

  fun requestStream() {
    val rpcService = rpcClient.getRpcService(RpcPlayerService::class.java, 1)
    rpcService.requestStream(8936832658046976).subscribe(
      { data ->
        logger.info("${LocalDateTime.now()}    requestStream: $data")
      },
      { e ->
        e.printStackTrace()
      }
    )
  }

  fun requestChannel() {
    val rpcService = rpcClient.getRpcService(RpcPlayerService::class.java, 1)
    val flux = Flux.interval(Duration.ofSeconds(2)).map { it.toInt() }
    rpcService.requestChannel(8936832658046976, flux)
      .subscribe(
        { data ->
          logger.info("${LocalDateTime.now()}    requestChannel: $data")
        },
        { e ->
          e.printStackTrace()
        }
      )
  }
}
