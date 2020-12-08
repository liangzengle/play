package play

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import play.util.reflect.isAbstract
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.toList

/**
 * 类扫描工具
 *
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
  fun <T> getOrdinarySubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return getOrdinarySubTypesSequence(superType).toSet()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getOrdinarySubTypesList(superType: Class<T>): List<Class<out T>> {
    return getOrdinarySubTypesStream(superType).toList()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getOrdinarySubTypesStream(superType: Class<T>): Stream<Class<out T>> {
    return reflections.getSubTypesOf(superType).stream()
      .filter { !it.isAbstract() && !it.isMemberClass && !it.isLocalClass && !it.isAnonymousClass }
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getOrdinarySubTypesSequence(superType: Class<T>): Sequence<Class<out T>> {
    return getOrdinarySubTypesStream(superType).asSequence()
  }

  /**
   * 获取[superType]的所有子类
   */
  fun <T> getAllSubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return reflections.getSubTypesOf(superType) as Set<Class<out T>>
  }
}
