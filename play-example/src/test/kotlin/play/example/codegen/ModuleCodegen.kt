package play.example.codegen

import com.squareup.kotlinpoet.*
import play.SystemProperties
import play.example.module.LogSource
import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode
import play.mvc.AbstractController
import play.mvc.Controller
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生成模块的模板代码
 *
 * @author LiangZengle
 */
object ModuleCodegen {

  val moduleName = "Friend"
  val moduleDesc = "好友"

  val modulePackage = "play.example.module"
  val moduleDir =
    Paths.get(SystemProperties.userDir() + "/play-example/src/main/kotlin/play/example/module/${moduleName.toLowerCase()}")

  private val ModuleId = ModuleId::class.java.asTypeName()

  @JvmStatic
  fun main(args: Array<String>) {
    val entityDir = moduleDir.resolve("entity")
    val configDir = moduleDir.resolve("config")
    val controllerDir = moduleDir.resolve("controller")
    val domainDir = moduleDir.resolve("domain")

    Files.createDirectories(entityDir)
    Files.createDirectories(configDir)
    Files.createDirectories(controllerDir)
    Files.createDirectories(domainDir)

    write("${modulePackage}.${moduleName.toLowerCase()}", moduleDir, createService())
    write("${modulePackage}.${moduleName.toLowerCase()}.domain", domainDir, createErrorCode())
    write("${modulePackage}.${moduleName.toLowerCase()}.domain", domainDir, createLogSource())
    write("${modulePackage}.${moduleName.toLowerCase()}.controller", controllerDir, createController())
  }

  private fun createErrorCode(): TypeSpec {
    val className = "${moduleName}ErrorCode"
    return TypeSpec.objectBuilder(className)
      .addKdoc("${moduleDesc}错误码")
      .superclass(StatusCode::class.java.asClassName())
      .addSuperclassConstructorParameter("%T.${moduleName}", ModuleId)
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
      .superclass(LogSource::class.java.asClassName())
      .addSuperclassConstructorParameter("%T.${moduleName}", ModuleId)
      .addAnnotation(
        AnnotationSpec.builder(ModularCode::class)
//          .addMember("%T.${moduleName}", ModuleId)
          .build()
      ).build()
  }

  private fun createService(): TypeSpec {
    val className = "${moduleName}Service"
    val classBuilder = TypeSpec.classBuilder(className)
    val entityCacheClass = toClassName("${modulePackage}.${moduleName.toLowerCase()}.entity.${moduleName}EntityCache")
    val cachePropertyName = "${moduleName.toLowerCase()}EntityCache"
    classBuilder
      .addKdoc("${moduleDesc}模块逻辑处理")
      .addAnnotation(Singleton::class)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject::class)
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
    val serviceClass = toClassName("${modulePackage}.${moduleName.toLowerCase()}.${moduleName}Service")
    val serviceName = "${moduleName.toLowerCase()}Service"
    classBuilder
      .addKdoc("${moduleDesc}模块请求处理")
      .addAnnotation(Singleton::class)
      .addAnnotation(
        AnnotationSpec.builder(Controller::class).addMember("%T.${moduleName}", ModuleId).build()
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject::class)
          .addParameter(serviceName, serviceClass)
          .build()
      )
      .addProperty(
        PropertySpec.builder(serviceName, serviceClass, KModifier.PRIVATE)
          .initializer(serviceName)
          .build()
      ).superclass(AbstractController::class)
      .addSuperclassConstructorParameter("%T.${moduleName}", ModuleId)

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
