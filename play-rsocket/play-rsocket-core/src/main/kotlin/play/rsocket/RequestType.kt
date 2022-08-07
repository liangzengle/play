package play.rsocket

/**
 *
 * @author LiangZengle
 */
enum class RequestType {

  FireAndForget,
  RequestResponse,
  RequestStream,
  RequestChannel
}
