package play.mvc

import play.util.ServiceLoader2

/**
 *
 * @author LiangZengle
 */
interface RequestBodyFactory {

  companion object {
    val Default: RequestBodyFactory =
      ServiceLoader2.loadOrDefault(RequestBodyFactory::class.java) { KProtobufRequestBodyFactory() }

    fun empty(): RequestBody = Default.empty()

    fun builder(): RequestBodyBuilder = Default.builder()

    fun parseFrom(bytes: ByteArray): RequestBody = Default.parseFrom(bytes)
  }

  fun empty(): RequestBody

  fun builder(): RequestBodyBuilder

  fun parseFrom(bytes: ByteArray): RequestBody
}
