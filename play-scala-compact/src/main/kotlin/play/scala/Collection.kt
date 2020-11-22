package play.scala

import play.util.Scala
import scala.collection.Seq
import scala.collection.immutable.Vector
import scala.jdk.javaapi.CollectionConverters

typealias ScalaVector<T> = Vector<T>
typealias ScalaList<T> = scala.collection.immutable.List<T>
typealias MutableSeq<T> = scala.collection.mutable.Seq<T>
typealias ScalaSet<T> = scala.collection.Set<T>
typealias ScalaMutableSet<T> = scala.collection.mutable.Set<T>
typealias ScalaMap<K, V> = scala.collection.Map<K, V>
typealias ScalaMutableMap<K, V> = scala.collection.mutable.Map<K, V>

fun <T> Array<T>.toScalaSeq(): scala.collection.immutable.Seq<T> = Scala.seqOf(this)

fun <T> Array<T>.toScalaVector(): ScalaVector<T> = Scala.vectorOf(this)

fun <T> Array<T>.toScalaList(): ScalaList<T> = Scala.listOf(this)

fun <K, V> Map<K, V>.asScala(): ScalaMap<K, V> = CollectionConverters.asScala(this)

fun <K, V> MutableMap<K, V>.asScalaMutable(): ScalaMutableMap<K, V> = CollectionConverters.asScala(this)

fun <T> ScalaVector<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> ScalaList<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> Seq<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> MutableSeq<T>.asJava(): MutableList<T> = CollectionConverters.asJava(this)
