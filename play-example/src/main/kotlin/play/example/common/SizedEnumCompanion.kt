package play.example.common

/**
 * Created by liang on 2020/6/27.
 */
interface SizedEnumCompanion<T : Enum<T>> {
  val elems: Array<T>

  fun size() = elems.size
}
