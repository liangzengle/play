package play.spring

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
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
    val scanner = Scanner()
    scanner.addIncludeFilter(AllMatch(AssignableTypeFilter(superType), InstantiatableFilter))
    return packagesToScan.asSequence().flatMap(scanner::findCandidateComponents)
      .map { Reflect.loadClass<T>(it.beanClassName!!, false, SpringClassScanner::class.java.classLoader) }.toSet()
  }

  override fun getInstantiatableClassesAnnotatedWith(annotationType: Class<out Annotation>): Set<Class<*>> {
    val scanner = Scanner()
    scanner.addIncludeFilter(AllMatch(AnnotationTypeFilter(annotationType), InstantiatableFilter))
    return packagesToScan.asSequence().flatMap(scanner::findCandidateComponents)
      .map { Reflect.loadClass<Any>(it.beanClassName!!, false, SpringClassScanner::class.java.classLoader) }.toSet()
  }

  class Scanner : ClassPathScanningCandidateComponentProvider(false) {
    override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
      return true
    }
  }

  class AllMatch(private val filters: List<TypeFilter>) : TypeFilter {
    constructor(vararg typeFilters: TypeFilter) : this(typeFilters.asList())

    override fun match(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean {
      return filters.all { it.match(metadataReader, metadataReaderFactory) }
    }
  }
}
