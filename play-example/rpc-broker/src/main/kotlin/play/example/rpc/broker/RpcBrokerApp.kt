package play.example.rpc.broker

import com.alibaba.rsocket.encoding.RSocketEncodingFacade
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class RpcBrokerApp

fun main(args: Array<String>) {
  //checking encoder first
  RSocketEncodingFacade.getInstance()
  SpringApplication.run(RpcBrokerApp::class.java, *args)
}
