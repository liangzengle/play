package play

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import play.util.reflect.isAbstract
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * Created by LiangZengle on 2020/2/16.
 */
class ClassScanner(packagesToScan: Collection<String>) {

  init {
    Log.info { "ClassScanner scanning packages: $packagesToScan" }
  }

  val reflections = Reflections(packagesToScan.toTypedArray(), SubTypesScanner(false))

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getSubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return getSubTypesSequence(superType).toSet()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getSubTypesList(superType: Class<T>): List<Class<out T>> {
    return getSubTypesStream(superType).asSequence().toList()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getSubTypesStream(superType: Class<T>): Stream<Class<out T>> {
    return reflections.getSubTypesOf(superType).stream()
      .filter { !it.isAbstract() && !it.isMemberClass && !it.isLocalClass && !it.isAnonymousClass }
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getSubTypesSequence(superType: Class<T>): Sequence<Class<out T>> {
    return getSubTypesStream(superType).asSequence()
  }

  /**
   * 获取[superType]的所有子类
   */
  fun <T> getAllSubTypesSet(superType: Class<T>): Set<Class<out T>> {
    return reflections.getSubTypesOf(superType) as Set<Class<out T>>
  }
}
