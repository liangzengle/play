package play

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import play.util.reflect.isAbstract
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.toList

/**
 * Created by LiangZengle on 2020/2/16.
 */
class ClassScanner(packagesToScan: Collection<String>) {
  constructor(vararg packagesToScan: String) : this(packagesToScan.asList())

  init {
    Log.info { "ClassScanner scanning packages: $packagesToScan" }
  }

  val reflections = Reflections(packagesToScan, SubTypesScanner(false))

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getConcreteSubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return getConcreteSubTypesSequence(superType).toSet()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getConcreteSubTypesList(superType: Class<T>): List<Class<out T>> {
    return getConcreteSubTypesStream(superType).toList()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getConcreteSubTypesStream(superType: Class<T>): Stream<Class<out T>> {
    return reflections.getSubTypesOf(superType).stream()
      .filter { !it.isAbstract() && !it.isMemberClass && !it.isLocalClass && !it.isAnonymousClass }
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getConcreteSubTypesSequence(superType: Class<T>): Sequence<Class<out T>> {
    return getConcreteSubTypesStream(superType).asSequence()
  }

  /**
   * 获取[superType]的所有子类
   */
  fun <T> getAllSubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return reflections.getSubTypesOf(superType) as Set<Class<out T>>
  }
}
