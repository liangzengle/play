package play.doc

import play.SystemProps
import play.doc.model.ModuleDescriptor
import play.doc.model.ProtocolDescriptor
import play.dokka.Dokka
import play.dokka.model.ParameterDescriptor
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.util.ClassUtil
import play.util.control.Result2
import play.util.json.Json
import play.util.reflect.ClassgraphClassScanner
import play.util.reflect.Reflect
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 *
 * @author LiangZengle
 */
object ClientProtocolGenerator {
  val projectDir = SystemProps.userDir() + "/play-example/game"

  val doc = Dokka.generate(File(projectDir))

  val classScanner = ClassgraphClassScanner(emptyList(), emptyList())

  @JvmStatic
  fun main(args: Array<String>) {
    val modules = modules()
    val statusCodes = statusCodes()
    val protocols = protocols { modules[it] }
    val modulesContent = Json.prettyWriter().writeValueAsString(modules)
    val statusCodesContent = Json.prettyWriter().writeValueAsString(statusCodes)
    val protocolsContent = Json.prettyWriter().writeValueAsString(protocols)
    Path("$projectDir/out").createDirectories().resolve("module.json").writeText(modulesContent)
    Path("$projectDir/out").createDirectories().resolve("statusCode.json").writeText(statusCodesContent)
    Path("$projectDir/out").createDirectories().resolve("protocol.json").writeText(protocolsContent)
  }

  fun modules(): Map<Int, String> {
    val map = TreeMap<Int, String>()
    for (field in ModuleId.javaClass.fields) {
      if (field.type != Short::class.java) {
        continue
      }
      val moduleId = field.get(ModuleId) as Short
      val desc = doc.query(ModuleId.javaClass)?.query(field)?.desc ?: field.name
      val prev = map.put(moduleId.toInt(), desc)
      if (prev != null) {
        throw IllegalStateException("模块id重复: $moduleId")
      }
    }
    return map
  }

  fun protocols(moduleDesc: (Int) -> String?): List<ModuleDescriptor> {
    val scanResult = classScanner.getInstantiatableSubclassInfoList(AbstractController::class.java)
    val result = ArrayList<ModuleDescriptor>(scanResult.size)
    for (classInfo in scanResult) {
      val controller = classInfo.getAnnotationInfo(Controller::class.java) ?: throw IllegalStateException()
      val moduleId = controller.parameterValues.getValue("moduleId") as Short
      val clazz = classInfo.loadClass()
      val protocols = (clazz.declaredMethods.asSequence()
        .filter { method -> method.isAnnotationPresent(Cmd::class.java) }
        .map { toProtocol(moduleId.toInt(), it) } +
        clazz.declaredFields.asSequence()
          .filter { field -> field.isAnnotationPresent(Cmd::class.java) }
          .map { toProtocol(moduleId.toInt(), it) }
        ).toList()
      val d = ModuleDescriptor(moduleId.toInt(), moduleDesc(moduleId.toInt()) ?: "", protocols)
      result.add(d)
    }
    return result
  }

  fun toProtocol(moduleId: Int, method: Method): ProtocolDescriptor {
    val cmd = method.getAnnotation(Cmd::class.java) ?: throw IllegalStateException()
    val cmdId = cmd.value
    val methodDescriptor = doc.query(method.declaringClass)?.query(method)
    val params = method.parameters.asSequence().mapIndexed { i, p ->
      val desc = methodDescriptor?.parameters?.get(i)?.desc ?: p.name
      ParameterDescriptor(p.name, p.parameterizedType.typeName, desc)
    }.dropWhile { it.type == PlayerManager.Self::class.qualifiedName }
      .toList()
    return ProtocolDescriptor(
      moduleId * 1000 + cmdId.toInt(),
      1,
      methodDescriptor?.desc ?: "",
      params,
      getReturnTypeName(method.genericReturnType),
      methodDescriptor?.returnDesc ?: ""
    )
  }

  fun toProtocol(moduleId: Int, filed: Field): ProtocolDescriptor {
    val cmd = filed.getAnnotation(Cmd::class.java) ?: throw IllegalStateException()
    val cmdId = cmd.value
    val fieldDescriptor = doc.query(filed.declaringClass)?.query(filed)
    return ProtocolDescriptor(
      moduleId * 1000 + cmdId.toInt(),
      2,
      fieldDescriptor?.desc ?: "",
      emptyList(),
      getReturnTypeName(filed.genericType),
      fieldDescriptor?.desc ?: ""
    )
  }

  fun statusCodes(): Map<Int, String> {
    val classes = classScanner.getInstantiatableSubclasses(StatusCode::class.java)
    val map = TreeMap<Int, String>()
    for (statusCodeClass in classes) {
      val instance = Reflect.getKotlinObjectOrNewInstance<Any>(statusCodeClass.name)
      for (field in statusCodeClass.declaredFields) {
        field.trySetAccessible()
        val code = when (val value = field.get(instance)) {
          is Result2.Err -> value.code
          is Int -> value
          else -> null
        } ?: continue
        val desc = doc.query(statusCodeClass)?.query(field)?.desc ?: "unknown"
        val prev = map.put(code, desc)
        if (prev != null) {
          throw IllegalStateException("错误码重复: $code")
        }
      }
    }
    return map
  }

  private fun getReturnTypeName(type: Type): String {
    val rawType = if (type is ParameterizedType) {
      Reflect.getRawClass<Any>(type.actualTypeArguments[0])
    } else {
      Reflect.getRawClass(type)
    }
    return ClassUtil.unwrap(rawType).name
  }
}
