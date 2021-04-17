package play.example.common.id

import java.util.*

/**
 * Created by liang on 2020/6/27.
 */
abstract class IntIdGenerator {

  abstract fun nextOrThrow(): Int

  abstract fun next(): OptionalInt

  abstract fun hasNext(): Boolean
}
