package play.util.enumration

/**
 * Created by liang on 2020/6/27.
 */
interface NamedEnum<T : Enum<T>> {
  fun getName(): String
}
