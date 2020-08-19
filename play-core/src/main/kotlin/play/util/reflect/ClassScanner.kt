package play.util.reflect

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.github.classgraph.ScanResult
import play.Log
import play.util.concurrent.CommonPool
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

/**
 * 类扫描工具
 *
 * Created by LiangZengle on 2020/2/16.
 */
class ClassScanner(scanExecutor: ExecutorService, jarsToScan: List<String>, packagesToScan: List<String>) {
  constructor(jarsToScan: List<String>, packagesToScan: List<String>) : this(CommonPool, jarsToScan, packagesToScan)

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
      Log.info { "ClassScanner(${Integer.toHexString(hashCode())}) running ${scanTimes}th class scan" }
      return ClassGraph()
        .acceptJars(*jarsToScan)
        .acceptPackages(*packagesToScan)
        .enableClassInfo()
        .enableAnnotationInfo()
        .scan(scanExecutor, 8)
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

  fun <T> getInstantiatableSubclasses(superType: Class<T>): List<Class<T>> {
    val classInfoList = if (superType.isInterface) {
      scanResult.getClassesImplementing(superType.name)
    } else {
      scanResult.getSubclasses(superType.name)
    }
    return classInfoList.standardClasses.filter(ClassInfoFilters.instantiatableClass()).loadClasses(superType)
  }

  fun getInstantiatableClassesAnnotatedWith(annotationType: Class<out Annotation>): List<Class<*>> {
    return scanResult.getClassesWithAnnotation(annotationType)
      .standardClasses
      .filter(ClassInfoFilters.instantiatableClass())
      .loadClasses()
  }
}
