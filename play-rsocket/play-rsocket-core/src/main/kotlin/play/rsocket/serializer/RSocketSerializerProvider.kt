package play.rsocket.serializer

/**
 *
 *
 * @author LiangZengle
 */
fun interface RSocketSerializerProvider {
  fun get(): RSocketSerializer
}
