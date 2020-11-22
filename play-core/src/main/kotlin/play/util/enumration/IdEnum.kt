package play.util.enumration

/**
 * Created by liang on 2020/6/27.
 */
interface IdEnum<T : Enum<T>> {
  val id: Int
}
