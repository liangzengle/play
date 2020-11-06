package play.example.common

/**
 * Created by liang on 2020/6/27.
 */
interface IdEnum<T : Enum<T>> {
  val id: Int
}
