package play.example.common.id

import java.util.*

/**
 * Created by liang on 2020/6/27.
 */
abstract class LongIdGenerator {

  abstract fun nextOrThrow(): Long

  abstract fun next(): OptionalLong

  abstract fun hasNext(): Boolean
}
