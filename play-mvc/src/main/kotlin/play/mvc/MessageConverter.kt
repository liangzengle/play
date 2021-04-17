package play.mvc

/**
 *
 * Convert PlayerRequest to a custom message
 *
 * @author LiangZengle
 */
abstract class MessageConverter {

  abstract fun convert(pr: PlayerRequest): Any?

}
