package play.hotswap

import play.Log
import play.util.ClassFileUtil
import play.util.collection.toImmutableMap
import play.util.io.FileMonitor
import java.io.File
import java.io.FileNotFoundException

/**
 *
 * @author LiangZengle
 */
class HotSwapWatcher(private val dir: File) {
  constructor(dir: String) : this(File(dir))

  init {
    if (!dir.exists()) {
      throw FileNotFoundException(dir.absolutePath)
    }
  }

  fun start() {
    FileMonitor.aggregatedBuilder()
      .watchFileOrDir(dir)
      .watchCreate()
      .watchModify()
      .onFileChange(::redefineClasses)
      .build()
      .start()
  }

  private fun redefineClasses(classFiles: Iterable<File>) {
    val classMap = classFiles.asSequence()
      .filter { it.name.endsWith(".class") }
      .map { it.readBytes() }
      .map { classFile -> ClassFileUtil.getClassName(classFile) to classFile }
      .toImmutableMap()
    val result = HotSwapAgent.redefineClasses(classMap)
    Log.info(result.toString())
  }
}
