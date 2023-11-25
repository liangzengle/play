package play.hotswap

import org.junit.jupiter.api.Assertions.assertEquals
import play.util.SystemProps
import play.util.ClassFileUtil
import java.io.File

/**
 *
 * @author LiangZengle
 */
fun main() {
  // should run with -Djdk.attach.allowAttachSelf=true
  val newClass = SystemProps.userDir() + "/play-core/src/test/kotlin/play/hotswap/HotSwapObj.hotswap"
  val obj = HotSwapObj()
  val v0 = obj.get()
  val classBytes = File(newClass).readBytes()
  val className = ClassFileUtil.getClassName(classBytes)
  val hotSwapResult = HotSwapAgent.redefineClasses(mapOf(className to classBytes))
  val v1 = obj.get()
  assertEquals(100, v0)
  assertEquals(1, v1)

  println(hotSwapResult)
}
