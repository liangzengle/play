package play.rsocket.client

/**
 *
 *
 * @author LiangZengle
 */
fun interface RSocketClientCustomizer {
  fun customize(builder: RSocketClientBuilder)
}
