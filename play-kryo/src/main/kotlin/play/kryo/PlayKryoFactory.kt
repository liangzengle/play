/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package play.kryo

import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.util.ListReferenceResolver
import com.esotericsoftware.kryo.util.MapReferenceResolver
import org.objenesis.strategy.StdInstantiatorStrategy
import org.slf4j.LoggerFactory

class PlayKryoFactory(private val settings: KryoSettings) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun create(): PlayKryo {
    val referenceResolver = if (settings.kryoReferenceMap) MapReferenceResolver() else ListReferenceResolver()
    val classResolver =
      if (settings.idStrategy == "incremental") KryoClassResolver(settings.implicitRegistrationLogging)
      else if (settings.resolveSubclasses) SubclassResolver()
      else DefaultClassResolver()
    val kryo = PlayKryo(classResolver, referenceResolver)
    // support deserialization of classes without no-arg constructors
    val instStrategy = kryo.instantiatorStrategy as DefaultInstantiatorStrategy
    instStrategy.fallbackInstantiatorStrategy = StdInstantiatorStrategy()
    kryo.instantiatorStrategy = instStrategy
    kryo.setOptimizedGenerics(false) // causes issue serializing classes extending generic base classes

    when (settings.serializerType) {
      "graph" -> kryo.references = true
      "nograph" -> kryo.references = false
      else -> throw IllegalStateException("Unknown serializer type: " + settings.serializerType)
    }

    // if explicit we require all classes to be registered explicitely
    kryo.isRegistrationRequired = settings.idStrategy == "explicit"

    val classLoader = Thread.currentThread().contextClassLoader

    // register configured class mappings and classes
    if (settings.idStrategy != "default") {
      for ((fqcn, idNum) in settings.classNameMappings) {
        val id = idNum.toInt()
        val result = runCatching { classLoader.loadClass(fqcn) }
        if (result.isSuccess) {
          kryo.register(result.getOrThrow(), id)
        } else {
          logger.warn("Class could not be loaded and/or registered: {} ", fqcn)
        }
      }

      for (classname in settings.classNames) {
        val result = runCatching { classLoader.loadClass(classname) }
        if (result.isSuccess) {
          kryo.register(result.getOrThrow())
        } else {
          logger.warn("Class could not be loaded and/or registered: {} ", classname)
        }
      }
    }

    PlayKryoCustomizer.customize(kryo)

    if (classResolver is SubclassResolver) {
      classResolver.enable()
    }

    return kryo
  }
}
