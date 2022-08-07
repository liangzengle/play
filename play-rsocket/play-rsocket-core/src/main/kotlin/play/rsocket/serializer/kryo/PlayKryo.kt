package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.*
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory
import com.esotericsoftware.kryo.serializers.ClosureSerializer
import com.esotericsoftware.kryo.util.Util
import com.esotericsoftware.minlog.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.util.*

class PlayKryo private constructor() : Kryo() {

  companion object {
    private val customizers = ServiceLoader.load(PlayKryoCustomizer::class.java).toList()

    @JvmStatic
    fun newInstance(): PlayKryo {
      val kryo = PlayKryo()
      customizers.forEach { it.customize(kryo) }
      return kryo
    }
  }


  private val defaultSerializerFactory: SerializerFactory<*> = FieldSerializerFactory()

  private val defaultSerializerFactories = KryoReflect.getDefaultSerializerFactoriesImmutableView(this)

  private val serializerResolvers = IdentityHashMap<Class<*>, ParameterizedTypeSerializerResolver<*>>()

  private val defaultSerializerResolvers = IdentityHashMap<Class<*>, ParameterizedTypeSerializerResolver<*>>()

  init {
    setDefaultSerializer(defaultSerializerFactory)
  }

  fun addDefaultSerializerResolver(rawType: Class<*>, resolver: ParameterizedTypeSerializerResolver<*>) {
    defaultSerializerResolvers[rawType] = resolver
  }

  fun addSerializerResolver(rawType: Class<*>, resolver: ParameterizedTypeSerializerResolver<*>) {
    serializerResolvers[rawType] = resolver
  }

  fun getSerializerResolver(rawType: Class<*>): ParameterizedTypeSerializerResolver<*>? {
    val resolver = serializerResolvers[rawType]
    if (resolver != null) {
      return resolver
    }
    for ((k, v) in defaultSerializerResolvers) {
      if (k.isAssignableFrom(rawType)) {
        addSerializerResolver(rawType, v)
        return v
      }
    }
    return DefaultSerializerResolver
  }

  fun getSerializer(type: Type): Serializer<out Any> {
    return when (type) {
      is Class<*> -> {
        if (!type.isArray) {
          getSerializer(type)
        } else {
          val serializer = GenericArraySerializer<Any>()
          serializer.componentType = type.componentType
          serializer.componentSerializer = getSerializer(type.componentType)
          serializer
        }
      }

      is ParameterizedType -> {
        val rawType = type.rawType as Class<*>
        getSerializerResolver(rawType)?.getSerializer(this, type) ?: getSerializer(rawType)
      }

      else -> throw IllegalArgumentException("Can not find Serializer for type: ${type.javaClass.name}")
    }
  }

  fun addDefaultSerializerFactory(type: Class<*>, factory: SerializerFactory<*>) {
    addDefaultSerializer(type, factory)
  }

  fun getDefaultSerializerFactory(type: Class<*>): SerializerFactory<*> {
    val serializerFactoryForAnnotation = getDefaultSerializerFactoryForAnnotatedType(type)
    if (serializerFactoryForAnnotation != null) return serializerFactoryForAnnotation

    for (i in defaultSerializerFactories.indices) {
      val entry = defaultSerializerFactories[i]
      if (entry.key.isAssignableFrom(type) && entry.value.isSupported(type)) {
        return entry.value
      }
    }
    return defaultSerializerFactory
  }

  private fun getDefaultSerializerFactoryForAnnotatedType(type: Class<*>): SerializerFactory<*>? {
    if (type.isAnnotationPresent(DefaultSerializer::class.java)) {
      val annotation = type.getAnnotation(DefaultSerializer::class.java) as DefaultSerializer
      return Util.newFactory(annotation.serializerFactory.java, annotation.value.java)
    }
    return null
  }

  fun getRegistration(type: Class<*>, registerImplicit: Boolean): Registration {
    var registration = classResolver.getRegistration(type)
    if (registration == null) {
      if (Proxy.isProxyClass(type)) {
        // If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
        registration = getRegistration(InvocationHandler::class.java)
      } else if (!type.isEnum && Enum::class.java.isAssignableFrom(type) && type != Enum::class.java) {
        // This handles an enum value that is an inner class, eg: enum A {b{}}
        var superClass: Class<*>? = type
        while (true) {
          superClass = superClass?.superclass
          if (superClass == null) break
          if (superClass.isEnum) {
            registration = classResolver.getRegistration(superClass)
            break
          }
        }
      } else if (EnumSet::class.java.isAssignableFrom(type)) registration =
        classResolver.getRegistration(EnumSet::class.java)
      else if (isClosure(type)) //
        registration = classResolver.getRegistration(ClosureSerializer.Closure::class.java)
      if (registration == null) {
        require(registerImplicit) { unregisteredClassMessage(type) }
        if (Log.WARN && warnUnregisteredClasses) Log.warn(unregisteredClassMessage(type))
        registration = FactoryRegistration(this, type, getDefaultSerializerFactory(type))
        classResolver.register(registration)
      }
    }
    return registration
  }
}
