package play.framework.node.gateway

interface GatewayService {

  fun login()

  fun logout(accountId: Long, reason: Int)


}
