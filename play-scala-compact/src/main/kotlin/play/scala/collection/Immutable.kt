package play.scala.collection

import scala.collection.immutable.*

/**
 *
 * @author LiangZengle
 */
object Immutable {
  val Seq: `Seq$` get() = `Seq$`.`MODULE$`

  val IndexedSeq: `IndexedSeq$` get() = `IndexedSeq$`.`MODULE$`

  val ArraySeq: `ArraySeq$` get() = `ArraySeq$`.`MODULE$`

  val Vector: `Vector$` get() = `Vector$`.`MODULE$`

  val List: `List$` get() = `List$`.`MODULE$`

  val Queue: `Queue$` get() = `Queue$`.`MODULE$`

  val Set: `Set$` get() = `Set$`.`MODULE$`

  val Map: `Map$` get() = `Map$`.`MODULE$`
}
