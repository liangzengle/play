package play.framework.node.gateway

import io.netty.buffer.ByteBuf
import play.framework.node.Session
import play.framework.node.gateway.message.HandshakeMessage
import play.framework.node.gateway.message.LoginMessage
import java.net.ServerSocket

open class GatewayController {

  fun handshake(message: HandshakeMessage) {
    val socket = ServerSocket().accept()
  }

  fun login(message: LoginMessage) {

  }

  fun dispatch(session: Session, messageType: Int, payload: ByteBuf) {

  }
}
