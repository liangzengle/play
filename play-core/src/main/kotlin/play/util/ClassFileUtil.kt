@file:JvmName("ClassFileUtil")

package play.util

import com.google.common.collect.Maps
import org.objectweb.asm.ClassReader
import java.io.InputStream

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
    val path = clazz.name.replace('.', '/') + ".class"
    return clazz.classLoader.getResourceAsStream(path)!!.readAllBytes()
  }

  fun getClassFiles(classes: Collection<Class<*>>): Map<Class<*>, ByteArray> {
    return Maps.toMap(classes, ::getClassFile)
  }
}
