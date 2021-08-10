package play.util.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.auto.service.AutoService
import play.util.createInstance
import java.io.IOException
import kotlin.reflect.KClass

abstract class AbstractTypeResolver<T : Any> {

  abstract fun resolve(node: ObjectNode): Class<out T>

  open fun recover(ex: Throwable): T? = null
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JsonAbstractType(val value: KClass<out AbstractTypeResolver<*>>)

@AutoService(Module::class)
class AbstractTypeResolveModule : Module() {
  override fun version(): Version = PackageVersion.VERSION

  override fun getModuleName(): String = "AbstractTypeResolveModule"

  override fun setupModule(context: SetupContext) {
    context.addDeserializers(AbstractTypeDeserializers())
  }
}

internal class AbstractTypeDeserializers : Deserializers.Base() {
  @Throws(JsonMappingException::class)
  override fun findBeanDeserializer(
    type: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer<*>? {
    return if (type.rawClass.isAnnotationPresent(JsonAbstractType::class.java)) {
      AbstractTypeDeserializer(type.rawClass)
    } else null
  }
}

internal class AbstractTypeDeserializer(rawClass: Class<*>) : StdDeserializer<Any?>(rawClass) {
  private val _typeResolver: AbstractTypeResolver<*> =
    rawClass.getAnnotation(JsonAbstractType::class.java).value.java.createInstance()

  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any {
    val codec = p.codec as ObjectMapper
    val node = codec.readTree<ObjectNode>(p)
    return try {
      val concreteType: Class<*> = _typeResolver.resolve(node)
      codec.convertValue(node, concreteType)
    } catch (ex: Exception) {
      _typeResolver.recover(ex) ?: throw ex
    }
  }
}

