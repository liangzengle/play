package play.example.common.scheduling

/**
 *
 * @author LiangZengle
 */
interface ScheduledEvent {

  override fun hashCode(): Int

  override fun equals(other: Any?): Boolean
}
