package play.event

/**
 *
 *
 * @author LiangZengle
 */
fun interface EventListener {

  fun eventReceive(): EventReceive

  fun newEventReceiveBuilder(): EventReceiveBuilder = EventReceiveBuilder()
}
