package play.scala.collection

import scala.collection.mutable.*

/**
 *
 * @author LiangZengle
 */
object Mutable {
  val Seq: `Seq$` get() = `Seq$`.`MODULE$`

  val IndexedSeq: `IndexedSeq$` get() = `IndexedSeq$`.`MODULE$`

  val ArraySeq: `ArraySeq$` get() = `ArraySeq$`.`MODULE$`

  val Queue: `Queue$` get() = `Queue$`.`MODULE$`

  val Stack: `Stack$` = `Stack$`.`MODULE$`

  val ListBuffer: `ListBuffer$` = `ListBuffer$`.`MODULE$`

  val Set: `Set$` get() = `Set$`.`MODULE$`

  val Map: `Map$` get() = `Map$`.`MODULE$`
}
