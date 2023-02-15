package play.hotswap

import com.sun.tools.attach.VirtualMachine
import play.util.ClassFileUtil
import java.io.File
import java.io.FileOutputStream
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.concurrent.withLock

/**
 *
 * @author LiangZengle
 */
object HotSwapAgent {
  const val AGENT_PATH = "hotswap-agent-path"

  private val defineClassMethod: Method by lazy {
    val method = ClassLoader::class.java.getDeclaredMethod(
      "defineClass",
      String::class.java,
      ByteArray::class.java,
      Int::class.java,
      Int::class.java
    )
    method.isAccessible = true
    method
  }

  @Volatile
  private var inst: Instrumentation? = null

  private val lock = ReentrantLock()

  @JvmStatic
  fun premain(agentArgs: String?, inst: Instrumentation) {
    agentmain(agentArgs, inst)
  }

  @JvmStatic
  @Suppress("UNUSED_PARAMETER")
  fun agentmain(agentArgs: String?, inst: Instrumentation) {
    this.inst = inst
  }

  fun redefineClasses(classes: Map<String, ByteArray>): HotSwapResult {
    if (classes.isEmpty()) {
      return HotSwapResult(emptyList(), emptyList())
    }
    lock.withLock {
      val inst = startAgent()
      val classLoader = Thread.currentThread().contextClassLoader
      val redefinedClasses = arrayListOf<String>()
      val definedClasses = arrayListOf<Class<*>>()
      val definitions = arrayListOf<ClassDefinition>()
      for ((name, content) in classes) {
        val existedClass = loadClassOrNull(name, classLoader)
        if (existedClass != null) {
          redefinedClasses.add(name)
          definitions.add(ClassDefinition(existedClass, content))
        } else {
          val newClass = defineNewClass(name, content, classLoader)
          definedClasses.add(newClass)
        }
      }
      inst.redefineClasses(*definitions.toTypedArray())
      return HotSwapResult(redefinedClasses, definedClasses)
    }
  }

  private fun loadClassOrNull(name: String, classLoader: ClassLoader): Class<*>? {
    return try {
      Class.forName(name, false, classLoader)
    } catch (e: ClassNotFoundException) {
      null
    }
  }

  private fun defineNewClass(name: String, content: ByteArray, classLoader: ClassLoader): Class<*> {
    defineClassMethod.invoke(classLoader, name, content, 0, content.size) as Class<*>
    return Class.forName(name, true, classLoader)
  }

  private fun startAgent(): Instrumentation {
    val value = this.inst
    if (value != null) {
      return value
    }

    val agentJar = createJarFile()
    val pid = ProcessHandle.current().pid()
    val vm = VirtualMachine.attach(pid.toString())
    vm.loadAgent(agentJar.absolutePath, null)
    vm.detach()

    // wait until instrumentation available
    for (i in 0..10) {
      val inst = this.inst
      if (inst != null) {
        return inst
      }
      try {
        Thread.sleep(1000)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        break
      }
    }
    throw IllegalStateException("hotswap agent not available")
  }

  private fun createJarFile(): File {
    val jarPath = System.getProperty(AGENT_PATH)
    if (jarPath != null) {
      val jarFile = File(jarPath)
      return if (!jarFile.exists()) createJarFile(jarFile) else jarFile
    }

    val jarFile = File.createTempFile("hotswap-agent", ".jar")
    jarFile.deleteOnExit()
    return createJarFile(jarFile)
  }

  private fun createJarFile(jar: File): File {
    val manifest = Manifest()
    val attrs = manifest.mainAttributes
    val className = HotSwapAgent::class.java.name
    attrs[Attributes.Name.MANIFEST_VERSION] = "1.0"
    attrs[Attributes.Name("Premain-Class")] = className
    attrs[Attributes.Name("Agent-Class")] = className
    attrs[Attributes.Name("Can-Retransform-Classes")] = "true"
    attrs[Attributes.Name("Can-Redefine-Classes")] = "true"
    JarOutputStream(FileOutputStream(jar), manifest).use { jos ->
      val e = JarEntry(className.replace('.', '/') + ".class")
      jos.putNextEntry(e)
      val content = ClassFileUtil.getClassFile(HotSwapAgent::class.java)
      jos.write(content)
      jos.closeEntry()
    }
    return jar
  }
}
