package play.scala.collection

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Awaitable, Promise}
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * ${class_desc}
 *
 * @author LiangZengle
 */
object Scala {

  def vectorOf[T](elem: T): Vector[T] = Vector(elem)

  def vectorOf[T <: AnyRef](elems: Array[T]): Vector[T] = Vector(elems: _*)

  def vectorOf[T](elems: java.lang.Iterable[T]): Vector[T] = elems.asScala.toVector

  def listOf[T](elem: T): List[T] = List(elem)

  def listOf[T <: AnyRef](elems: Array[T]): List[T] = List(elems: _*)

  def listOf[T](elems: java.lang.Iterable[T]): List[T] = elems.asScala.toList

  def seqOf[T](elem: T): Seq[T] = Seq(elem)

  def seqOf[T <: AnyRef](elems: Array[T]): Seq[T] = ArraySeq.unsafeWrapArray(elems)

  def seqOf[T](elems: java.lang.Iterable[T]): Seq[T] = elems.asScala.toSeq

  def setOf[T](elem: T): Set[T] = Set(elem)

  def setOf[T <: AnyRef](elems: Array[T]): Set[T] = Set(elems: _*)

  def setOf[T](elems: java.lang.Iterable[T]): Set[T] = elems.asScala.toSet

  def mapOf[K, V](key: K, value: V): Map[K, V] = Map(key -> value)

  def mapOf[K, V](pair: java.util.Map.Entry[K, V]): Map[K, V] = Map(pair.getKey -> pair.getValue)

  def promise[T](): Promise[T] = Promise()

  def await[T](awaitable: Awaitable[T], timeoutMillis: Long): Unit = {
    Await.ready(awaitable, FiniteDuration.apply(timeoutMillis, TimeUnit.MILLISECONDS))
  }

  def get[T](awaitable: Awaitable[T], timeoutMillis: Long): T = {
    Await.result(awaitable, FiniteDuration.apply(timeoutMillis, TimeUnit.MILLISECONDS))
  }
}
