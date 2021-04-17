package play.util.reflect

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.streams.toList
import play.Log

/**
 * 类扫描工具
 *
 * Created by LiangZengle on 2020/2/16.
 */
class ClassScanner(scanExecutor: ExecutorService, jarsToScan: List<String>, packagesToScan: List<String>) {
  private val weakScanResult = WeakReferenceScanResult(scanExecutor, jarsToScan, packagesToScan)

  private val scanResult get() = weakScanResult.get()

  private class WeakReferenceScanResult(
    private val scanExecutor: ExecutorService,
    private val jarsToScan: List<String>,
    private val packagesToScan: List<String>
  ) {
    private var ref = WeakReference(scan())
    private var scanTimes = 0

    fun get(): ScanResult {
      var value = ref.get()
      if (value != null) {
        return value
      }
      synchronized(this) {
        value = ref.get()
        if (value == null) {
          value = scan()
          ref = WeakReference(value)
        }
      }
      return value!!
    }

    private fun scan(): ScanResult {
      scanTimes++
      Log.info { "ClassScanner running {$scanTimes}th class scan" }
      return ClassGraph()
        .acceptJars(*jarsToScan.toTypedArray())
        .acceptPackages(*packagesToScan.toTypedArray())
        .enableClassInfo()
        .scan(scanExecutor, 8)
    }
  }

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
    return getOrdinarySubTypesSequence(superType).asStream()
  }

  /**
   * 获取[superType]的子类（不包括：MemberClass\LocalClass\AnonymousClass）
   */
  fun <T> getOrdinarySubTypesSequence(superType: Class<T>): Sequence<Class<out T>> {
    return (if (superType.isInterface) {
      scanResult.getClassesImplementing(superType.name)
    } else {
      scanResult.getSubclasses(superType.name)
    })
      .standardClasses
      .filter {
        !it.isAbstract && !it.isAnonymousInnerClass && !it.isInnerClass
      }
      .asSequence()
      .map { it.loadClass(superType) }
  }
}
