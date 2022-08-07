package play.rsocket.serializer

/**
 *
 *
 * @author LiangZengle
 */
fun interface PlaySerializerProvider {
  fun get(): PlaySerializer
}
