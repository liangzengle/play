package play.util.reflect

import com.google.common.base.Stopwatch
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.github.classgraph.ScanResult
import play.util.concurrent.CommonPool
import play.util.logging.WithLogger
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * 类扫描工具
 *
 * Created by LiangZengle on 2020/2/16.
 */
class ClassgraphClassScanner(scanExecutor: ExecutorService, jarsToScan: List<String>, packagesToScan: List<String>) :
  ClassScanner {
  constructor(jarsToScan: List<String>, packagesToScan: List<String>) : this(CommonPool, jarsToScan, packagesToScan)

  companion object : WithLogger()

  private val weakScanResult =
    WeakReferenceScanResult(scanExecutor, jarsToScan.toTypedArray(), packagesToScan.toTypedArray())

  val scanResult get() = weakScanResult.get()

  private class WeakReferenceScanResult(
    private val scanExecutor: ExecutorService,
    private val jarsToScan: Array<String>,
    private val packagesToScan: Array<String>
  ) {
    @Volatile
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
      val stopwatch = Stopwatch.createStarted()
      val result = ClassGraph()
        .acceptJars(*jarsToScan)
        .acceptPackages(*packagesToScan)
        .enableClassInfo()
        .enableAnnotationInfo()
        .scan(scanExecutor, 8)
      val elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS)
      logger.info { "ClassScanner(${Integer.toHexString(hashCode())}) run ${scanTimes}th class scan, cost ${elapsed}ms" }
      return result
    }
  }

  fun <T> getInstantiatableSubclassInfoList(superType: Class<T>): ClassInfoList {
    val classInfoList = if (superType.isInterface) {
      scanResult.getClassesImplementing(superType.name)
    } else {
      scanResult.getSubclasses(superType.name)
    }
    return classInfoList.standardClasses.filter(ClassInfoFilters.instantiatableClass())
  }

  override fun <T> getInstantiatableSubclasses(superType: Class<T>): List<Class<T>> {
    val classInfoList = if (superType.isInterface) {
      scanResult.getClassesImplementing(superType.name)
    } else {
      scanResult.getSubclasses(superType.name)
    }
    return classInfoList.standardClasses.filter(ClassInfoFilters.instantiatableClass()).loadClasses(superType)
  }

  override fun getInstantiatableClassesAnnotatedWith(annotationType: Class<out Annotation>): List<Class<*>> {
    return scanResult.getClassesWithAnnotation(annotationType)
      .standardClasses
      .filter(ClassInfoFilters.instantiatableClass())
      .loadClasses()
  }
}
