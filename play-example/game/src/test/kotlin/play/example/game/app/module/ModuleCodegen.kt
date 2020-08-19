package play.example.game.app.module

import com.squareup.kotlinpoet.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.SystemProps
import play.example.common.LogSource
import play.example.common.ModularCode
import play.example.common.StatusCode
import play.mvc.AbstractController
import play.mvc.Controller
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * 生成模块的模板代码
 *
 * @author LiangZengle
 */
object ModuleCodegen {

  val moduleName = "ServerTask"
  val moduleDesc = "全服任务"

  val modulePackage = "play.example.game.app.module"
  val moduleDir =
    Paths.get(SystemProps.userDir() + "/play-example/game/src/main/kotlin/play/example/game/app/module/${moduleName.lowercase()}")

  private val ModuleId = ModuleId::class.java.asTypeName()

  @JvmStatic
  fun main(args: Array<String>) {
    val entityDir = moduleDir.resolve("entity")
    val configDir = moduleDir.resolve("res")
    val domainDir = moduleDir.resolve("domain")

    Files.createDirectories(entityDir)
    Files.createDirectories(configDir)
    Files.createDirectories(domainDir)

    write("$modulePackage.${moduleName.lowercase()}", moduleDir, createService())
    write("$modulePackage.${moduleName.lowercase()}.domain", domainDir, createErrorCode())
    write("$modulePackage.${moduleName.lowercase()}.domain", domainDir, createLogSource())
    write("$modulePackage.${moduleName.lowercase()}", moduleDir, createController())
  }

  private fun createErrorCode(): TypeSpec {
    val className = "${moduleName}ErrorCode"
    return TypeSpec.objectBuilder(className)
      .addKdoc("${moduleDesc}错误码")
      .superclass(StatusCode::class)
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)
      .addAnnotation(
        AnnotationSpec.builder(ModularCode::class)
//          .addMember("%T.${moduleName}", ModuleId)
          .build()
      ).build()
  }

  private fun createLogSource(): TypeSpec {
    val className = "${moduleName}LogSource"
    return TypeSpec.objectBuilder(className)
      .addKdoc("${moduleDesc}日志源")
      .superclass(LogSource::class)
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)
      .addAnnotation(
        AnnotationSpec.builder(ModularCode::class)
//          .addMember("%T.${moduleName}", ModuleId)
          .build()
      ).build()
  }

  private fun createService(): TypeSpec {
    val className = "${moduleName}Service"
    val classBuilder = TypeSpec.classBuilder(className)
    val entityCacheClass = toClassName("$modulePackage.${moduleName.lowercase()}.entity.${moduleName}EntityCache")
    val cachePropertyName = "${moduleName.replaceFirstChar { it.lowercaseChar() }}EntityCache"
    classBuilder
      .addKdoc("${moduleDesc}模块逻辑处理")
      .addAnnotation(Component::class)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Autowired::class)
          .addParameter(cachePropertyName, entityCacheClass)
          .build()
      )
      .addProperty(
        PropertySpec.builder(cachePropertyName, entityCacheClass, KModifier.PRIVATE)
          .initializer(cachePropertyName)
          .build()
      )

    return classBuilder.build()
  }

  private fun createController(): TypeSpec {
    val className = "${moduleName}Controller"
    val classBuilder = TypeSpec.classBuilder(className)
    val serviceClass = toClassName("$modulePackage.${moduleName.lowercase()}.${moduleName}Service")
    val serviceName = "${moduleName.replaceFirstChar { it.lowercaseChar() }}Service"
    classBuilder
      .addKdoc("${moduleDesc}模块请求处理")
      .addAnnotation(Component::class)
      .addAnnotation(
        AnnotationSpec.builder(Controller::class).addMember("%T.$moduleName", ModuleId).build()
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Autowired::class)
          .addParameter(serviceName, serviceClass)
          .build()
      )
      .addProperty(
        PropertySpec.builder(serviceName, serviceClass, KModifier.PRIVATE)
          .initializer(serviceName)
          .build()
      ).superclass(AbstractController::class)
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)

    return classBuilder.build()
  }

  private fun toClassName(className: String): ClassName = ClassName.bestGuess(className)

  private fun write(pkg: String, dir: Path, clazz: TypeSpec) {
    val path = dir.resolve(clazz.name + ".kt")
    val stringWriter = StringWriter()
    FileSpec.builder(pkg, clazz.name!!)
      .addType(clazz)
      .build()
      .writeTo(stringWriter)
    try {
      Files.write(path, stringWriter.toString().toByteArray(), StandardOpenOption.CREATE_NEW)
    } catch (e: java.nio.file.FileAlreadyExistsException) {
      println("文件已经存在: $path")
    }
  }
}
