package play.rsocket.security

import io.rsocket.SocketAcceptor
import io.rsocket.plugins.SocketAcceptorInterceptor

/**
 *
 *
 * @author LiangZengle
 */
class SimpleTokenSocketAcceptorInterceptor(private val token: String) : SocketAcceptorInterceptor {
  override fun apply(acceptor: SocketAcceptor): SocketAcceptor {
    return SimpleTokenSocketAcceptor(acceptor, token)
  }
}
