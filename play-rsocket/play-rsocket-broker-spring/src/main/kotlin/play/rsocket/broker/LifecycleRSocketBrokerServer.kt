package play.rsocket.broker

import org.springframework.context.SmartLifecycle
import java.net.InetSocketAddress

/**
 *
 *
 * @author LiangZengle
 */
class LifecycleRSocketBrokerServer(private val delegate: RSocketBrokerServer) : RSocketBrokerServer, SmartLifecycle {
  override fun start() {
    delegate.start()
  }

  override fun stop() {
    delegate.stop()
  }

  override fun address(): InetSocketAddress? {
    return delegate.address()
  }

  override fun isRunning(): Boolean {
    return address() != null
  }
}
