import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import play.example.game.app.module.account.controller.AccountController
import play.mvc.Controller
import play.util.concurrent.CommonPool
import play.util.reflect.ClassScanner

object Generator {
  private val classFilePattern = "**/*.class"
  private val resourcePatternResolver = PathMatchingResourcePatternResolver()
  private val metadataReaderFactory = CachingMetadataReaderFactory(resourcePatternResolver)

  @JvmStatic
  fun main(args: Array<String>) {
    ClassScanner(
      CommonPool,
      emptyList(),
      listOf("play.example")
    ).scanResult
      .getClassesWithAnnotation(Controller::class.java.name)
      .forEach {
        val controller = it.getAnnotationInfo(Controller::class.java.name)
        println(controller)
      }

    val scanPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/play/example/" + classFilePattern
    val resources = resourcePatternResolver.getResources(scanPath)
    for (resource in resources) {
      val reader = metadataReaderFactory.getMetadataReader(resource)
      val classMetadata = reader.classMetadata
      val annotationMetadata = reader.annotationMetadata
      if (classMetadata.className.equals(AccountController::class.java.name)) {
        println(annotationMetadata.hasAnnotation(Controller::class.java.name))
      }

      if (!annotationMetadata.hasAnnotation(Controller::class.java.name)) {
        continue
      }
      val annotationAttributes = annotationMetadata.getAnnotationAttributes(Controller::class.java.name)!!
      println(annotationAttributes)
      val moduleId = annotationAttributes["moduleId"]
      println(moduleId)
    }
  }
}
