package play.scala

import play.scala.collection.Immutable
import scala.collection.immutable.Vector
import scala.jdk.javaapi.CollectionConverters

typealias ScalaVector<T> = Vector<T>
typealias ScalaList<T> = scala.collection.immutable.List<T>
typealias ScalaSeq<T> = scala.collection.Seq<T>
typealias ScalaMutableSeq<T> = scala.collection.mutable.Seq<T>
typealias ScalaSet<T> = scala.collection.Set<T>
typealias ScalaMutableSet<T> = scala.collection.mutable.Set<T>
typealias ScalaMap<K, V> = scala.collection.Map<K, V>
typealias ScalaMutableMap<K, V> = scala.collection.mutable.Map<K, V>
typealias ScalaIterable<T> = scala.collection.Iterable<T>

fun <T> Array<T>.asScalaSeq(): scala.collection.immutable.Seq<T> = Immutable.ArraySeq.unsafeWrapArray(this)

fun <T> Array<T>.asScalaVector() = Immutable.Vector.apply(asScalaSeq())

fun <T> Array<T>.asScalaList() = Immutable.List.apply(asScalaSeq())

fun <T> ScalaVector<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> ScalaList<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> ScalaSeq<T>.asJava(): List<T> = CollectionConverters.asJava(this)

fun <T> ScalaMutableSeq<T>.asJava(): MutableList<T> = CollectionConverters.asJava(this)

fun <T> Iterable<T>.asScala(): ScalaIterable<T> = CollectionConverters.asScala(this)

fun <T> List<T>.asScala(): ScalaSeq<T> = CollectionConverters.asScala(this)

fun <T> MutableList<T>.asScala(): ScalaMutableSeq<T> = CollectionConverters.asScala(this)

fun <T> Set<T>.asScala(): ScalaSet<T> = CollectionConverters.asScala(this)

fun <T> MutableSet<T>.asScala(): ScalaMutableSet<T> = CollectionConverters.asScala(this)

fun <K, V> Map<K, V>.asScala(): ScalaMap<K, V> = CollectionConverters.asScala(this)

fun <K, V> MutableMap<K, V>.asScala(): ScalaMutableMap<K, V> = CollectionConverters.asScala(this)
