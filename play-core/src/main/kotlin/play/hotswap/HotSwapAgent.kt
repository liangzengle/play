package play.hotswap

import com.sun.tools.attach.VirtualMachine
import play.util.ClassFileUtil
import java.io.File
import java.io.FileOutputStream
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 *
 * @author LiangZengle
 */
object HotSwapAgent {
  private val defineClassMethod: Method by lazy {
    val method = ClassLoader::class.java.getDeclaredMethod(
      "defineClass",
      String::class.java,
      ByteArray::class.java,
      Int::class.java,
      Int::class.java
    )
    if (!method.trySetAccessible()) {
      method.isAccessible = true
    }
    method
  }

  private var instrumentation: Instrumentation? = null

  @JvmStatic
  fun premain(agentArgs: String?, inst: Instrumentation) {
    agentmain(agentArgs, inst)
  }

  @JvmStatic
  fun agentmain(agentArgs: String?, inst: Instrumentation) {
    instrumentation = inst
  }

  fun redefineClasses(classes: Map<String, ByteArray>): HotSwapResult {
    if (classes.isEmpty()) {
      return HotSwapResult(emptyList(), emptyList())
    }
    startAgent()
    val inst = instrumentation!!
    val classLoader = Thread.currentThread().contextClassLoader
    val definitions = arrayOfNulls<ClassDefinition>(classes.size)
    val redefinedClasses = arrayListOf<String>()
    val definedClasses = arrayListOf<String>()
    var i = 0
    for ((name, content) in classes) {
      val loadedClass = loadClassOrNull(name, classLoader)
      val clazz = loadedClass ?: defineNewClass(name, content, classLoader)
      if (loadedClass != null) {
        redefinedClasses.add(name)
      } else {
        definedClasses.add(name)
      }
      definitions[i++] = ClassDefinition(clazz, content)
    }
    inst.redefineClasses(*definitions)
    return HotSwapResult(redefinedClasses, definedClasses)
  }

  private fun loadClassOrNull(name: String, classLoader: ClassLoader): Class<*>? {
    return try {
      Class.forName(name, false, classLoader)
    } catch (e: ClassNotFoundException) {
      null
    }
  }

  private fun defineNewClass(name: String, content: ByteArray, classLoader: ClassLoader): Class<*> {
    val clazz = defineClassMethod.invoke(classLoader, name, content, 0, content.size) as Class<*>
    Class.forName(name, true, classLoader)
    return clazz
  }

  private fun startAgent() {
    if (instrumentation != null) {
      return
    }

    val agentJar = createJarFile()
    val pid = ProcessHandle.current().pid()
    val vm: VirtualMachine = VirtualMachine.attach(pid.toString())
    vm.loadAgent(agentJar.absolutePath, null)
    vm.detach()

    // wait until instrumentation available
    for (i in 0..10) {
      if (instrumentation != null) {
        return
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
    val jarPath = System.getProperty("hotswap-agent-jar-path")
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
