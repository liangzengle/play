@file:JvmName("ClassFileUtil")

package play.util

import io.github.classgraph.ClassGraph
import org.objectweb.asm.ClassReader
import play.util.collection.toImmutableMap
import java.io.InputStream
import kotlin.io.path.readBytes
import kotlin.io.path.toPath

/**
 *
 * @author LiangZengle
 */
object ClassFileUtil {

  fun getClassName(classFile: ByteArray): String {
    return ClassReader(classFile).className.replace('/', '.')
  }

  fun getClassName(classFile: InputStream): String {
    return ClassReader(classFile).className.replace('/', '.')
  }

  fun getClassFile(clazz: Class<*>): ByteArray {
    ClassGraph().acceptClasses(clazz.name).scan().use {
      return it.getClassInfo(clazz.name).resource.uri.toPath().readBytes()
    }
  }

  fun getClassFiles(classes: Collection<Class<*>>): Map<Class<*>, ByteArray> {
    val nameToClass = classes.toImmutableMap { it.name }
    val names = nameToClass.keys.toTypedArray()
    val result = hashMapOf<Class<*>, ByteArray>()
    ClassGraph().acceptClasses(*names).scan().use { scanResult ->
      for (classInfo in scanResult.allClasses) {
        val clazz = nameToClass[classInfo.name] ?: continue
        result[clazz] = classInfo.resource.uri.toPath().readBytes()
      }
    }
    return result
  }
}
