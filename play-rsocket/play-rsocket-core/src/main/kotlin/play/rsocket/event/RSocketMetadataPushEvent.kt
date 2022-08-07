package play.rsocket.event

/**
 *
 * @author LiangZengle
 */
class RSocketMetadataPushEvent(val type: String, val data: Any) {
  override fun toString(): String {
    return "RSocketMetaPushEvent($type($data))"
  }
}
