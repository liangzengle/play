package play.mvc

/**
 *
 * convert PlayerRequest to a custom message
 *
 * @author LiangZengle
 */
abstract class MessageConverter {

  abstract fun convert(pr: PlayerRequest): Any?

}
