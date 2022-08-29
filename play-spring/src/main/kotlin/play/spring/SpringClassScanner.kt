package play.spring

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.core.type.filter.TypeFilter
import play.util.reflect.ClassScanner
import play.util.reflect.Reflect

/**
 * A [ClassScanner] base on [ClassPathScanningCandidateComponentProvider]
 *
 * @author LiangZengle
 */
class SpringClassScanner(private val packagesToScan: List<String>) : ClassScanner {
  companion object {
    private val InstantiatableFilter = TypeFilter { metadataReader, _ ->
      val metadata = metadataReader.classMetadata
      metadata.isConcrete && metadata.isIndependent
    }
  }

  override fun <T> getInstantiatableSubclasses(superType: Class<T>): Set<Class<T>> {
    val provider = ClassPathScanningCandidateComponentProvider(true)
    provider.addIncludeFilter(AssignableTypeFilter(superType))
    provider.addIncludeFilter(InstantiatableFilter)
    return packagesToScan.asSequence().flatMap(provider::findCandidateComponents)
      .map { Reflect.loadClass<T>(it.beanClassName!!) }.toSet()
  }

  override fun getInstantiatableClassesAnnotatedWith(annotationType: Class<out Annotation>): Set<Class<*>> {
    val provider = ClassPathScanningCandidateComponentProvider(true)
    provider.addIncludeFilter(AnnotationTypeFilter(annotationType))
    provider.addIncludeFilter(InstantiatableFilter)
    return packagesToScan.asSequence().flatMap(provider::findCandidateComponents)
      .map { Reflect.loadClass<Any>(it.beanClassName!!) }.toSet()
  }
}
