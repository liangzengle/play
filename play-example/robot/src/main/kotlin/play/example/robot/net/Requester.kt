package play.example.robot.net

import com.github.benmanes.caffeine.cache.Caffeine
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request
import play.mvc.RequestBodyFactory
import java.time.Duration

class Requester(private val client: RobotClient) {

  private var requestId = 0

  private val requestParamsCache =
    Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).build<Int, RequestParams>()

  private fun nextRequestId(): Int {
    requestId++
    if (requestId < 0) {
      requestId = 1
    }
    return requestId
  }

  fun send(msgId: Int, params: RequestParams?) {
    val requestId = nextRequestId()
    val header = Header(MsgId(msgId), requestId)
    client.write(Request(header, params?.toRequestBody() ?: RequestBodyFactory.Default.empty()))
    if (params != null) {
      requestParamsCache.put(requestId, params)
    }
  }


  fun <T : RequestParams> getRequestParams(requestId: Int): T? {
    @Suppress("UNCHECKED_CAST")
    return requestParamsCache.asMap().remove(requestId) as? T
  }

}
