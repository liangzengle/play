package play.example.rpc.test

import org.springframework.stereotype.Component
import play.example.rpc.api.RpcPlayerService
import play.rsocket.rpc.RpcClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerNameRequester(private val rpcClient: RpcClient) {

  @PostConstruct
  private fun init() {
    val executor = Executors.newSingleThreadScheduledExecutor()
    executor.scheduleWithFixedDelay(::test, 10, 2, TimeUnit.SECONDS)
  }

  fun test() {
//    try {
//      val service = rpcClient.getService(RpcPlayerService::class)
//      val playerName = service.getPlayerName(1).blockingGet()
//      println("playerName: $playerName")
//    } catch (e: Exception) {
//      e.printStackTrace()
//    }

    try {
      val service = rpcClient.getService(RpcPlayerService::class.java, 1.toString())
      val playerName = service.getPlayerName(8936832658046976).blockingGet()
      println("playerName1: $playerName")
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      val service = rpcClient.getService(RpcPlayerService::class.java, 2.toString())
      val playerName = service.getPlayerName(2).blockingGet()
      println("playerName2: $playerName")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
