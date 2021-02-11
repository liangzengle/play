package play.example.codegen

import com.squareup.kotlinpoet.*
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import javax.inject.Singleton
import play.SystemProps
import play.config.AbstractConfig
import play.example.module.LogSource
import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode
import play.example.module.player.entity.AbstractPlayerEntity
import play.mvc.AbstractController
import play.mvc.Controller

/**
 * 生成模块的模板代码
 *
 * @author LiangZengle
 */
object ModuleCodegen {

  private val moduleName = "Task"
  private val moduleDesc = "任务基础"

  private val modulePackage = "play.example.module"
  private val moduleDir =
    Paths.get(SystemProps.userDir() + "/play-example/src/main/kotlin/play/example/module/${moduleName.toLowerCase()}")
  private val protoDir =
    Paths.get(SystemProps.userDir() + "/play-example/src/main/proto")

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

    write("$modulePackage.${moduleName.toLowerCase()}", moduleDir, createService())
    write("$modulePackage.${moduleName.toLowerCase()}.domain", domainDir, createErrorCode())
    write("$modulePackage.${moduleName.toLowerCase()}.domain", domainDir, createLogSource())
    write("$modulePackage.${moduleName.toLowerCase()}", controllerDir, createController())
    write("$modulePackage.${moduleName.toLowerCase()}.entity", entityDir, createEntity())
    write("$modulePackage.${moduleName.toLowerCase()}.config", configDir, createConfig())

    createProtoFile()
  }

  private fun createConfig(): TypeSpec {
    val className = "${moduleName}Config"
    return TypeSpec.classBuilder(className)
      .addKdoc("${moduleDesc}配置")
      .superclass(AbstractConfig::class)
      .build()
  }

  private fun createEntity(): TypeSpec {
    val className = "${moduleName}Entity"
    return TypeSpec.classBuilder(className)
      .primaryConstructor(FunSpec.constructorBuilder().addParameter("playerId", Long::class).build())
      .addKdoc("${moduleDesc}数据")
      .superclass(AbstractPlayerEntity::class)
      .addSuperclassConstructorParameter("playerId")
      .build()
  }

  private fun createErrorCode(): TypeSpec {
    val className = "${moduleName}ErrorCode"
    return TypeSpec.objectBuilder(className)
      .addKdoc("${moduleDesc}错误码")
      .superclass(StatusCode::class)
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)
//      .addAnnotation(ModularCode::class)
      .addAnnotation(
        AnnotationSpec.builder(SuppressWarnings::class).addMember("%S", "MayBeConstant").build()
      )
      .build()
  }

  private fun createLogSource(): TypeSpec {
    val className = "${moduleName}LogSource"
    return TypeSpec.objectBuilder(className)
      .addKdoc("${moduleDesc}日志源")
      .superclass(LogSource::class)
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)
      .addAnnotation(ModularCode::class)
      .addAnnotation(
        AnnotationSpec.builder(SuppressWarnings::class).addMember("%S", "MayBeConstant").build()
      )
      .build()
  }

  private fun createService(): TypeSpec {
    val entityCacheClassName = if (moduleName.endsWith("Entity")) moduleName + "Cache"
    else if (moduleName.endsWith("Data")) moduleName.substring(0, moduleName.length - 4) + "EntityCache"
    else moduleName + "EntityCache"

    val className = "${moduleName}Service"
    val classBuilder = TypeSpec.classBuilder(className)
    val entityCacheClass = toClassName("$modulePackage.${moduleName.toLowerCase()}.entity.$entityCacheClassName")
    val cachePropertyName = "${moduleName.decapitalize()}EntityCache"
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
    val serviceClass = toClassName("$modulePackage.${moduleName.toLowerCase()}.${moduleName}Service")
    val serviceName = "${moduleName.decapitalize()}Service"
    classBuilder
      .addKdoc("${moduleDesc}模块请求处理")
      .addAnnotation(Singleton::class)
      .addAnnotation(
        AnnotationSpec.builder(Controller::class).addMember("%T.$moduleName", ModuleId).build()
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
      .addSuperclassConstructorParameter("%T.$moduleName", ModuleId)

    return classBuilder.build()
  }

  private fun toClassName(className: String): ClassName = ClassName.bestGuess(className)

  private fun createProtoFile() {
    val pkg = "play.example.module.${moduleName.toLowerCase()}.message"
    val content =
      """
      syntax = "proto3";

      package $pkg;
      """.trimIndent()
    val path = protoDir.resolve(toSnakeLike(moduleName) + ".proto")
    Files.write(path, content.toByteArray(), StandardOpenOption.CREATE)
  }

  private fun toSnakeLike(input: String): String {
    val b = StringBuilder()
    for (i in input.indices) {
      val c = input[i]
      if (b.isNotEmpty() && c.isUpperCase()) {
        b.append('_')
      }
      b.append(c.toLowerCase())
    }
    return b.toString()
  }

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
