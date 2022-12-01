package play.rsocket.broker

import reactor.netty.tcp.TcpServer
import java.util.function.Function

/**
 * @author LiangZengle
 */
fun interface TcpServerOperator : Function<TcpServer, TcpServer>
