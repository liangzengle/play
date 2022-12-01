package play.rsocket.client

import reactor.netty.tcp.TcpClient
import java.util.function.Function

/**
 * @author LiangZengle
 */
fun interface TcpClientOperator : Function<TcpClient, TcpClient>
