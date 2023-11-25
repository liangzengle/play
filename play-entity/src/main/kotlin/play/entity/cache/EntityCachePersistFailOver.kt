package play.entity.cache


import java.io.File

interface EntityCachePersistFailOver {
  fun onPersistFailed(entityCache: EntityCache<*, *>)
}

object NOOPEntityCachePersistFailOver : EntityCachePersistFailOver {
  override fun onPersistFailed(entityCache: EntityCache<*, *>) {
  }
}

class DefaultEntityCachePersistFailOver(dumpPath: String) : EntityCachePersistFailOver {

  companion object : WithLogger()

  private val dumpDir = File(dumpPath)

  init {
    checkUnhandledCacheDump()
  }

  override fun onPersistFailed(entityCache: EntityCache<*, *>) {
    cacheDump(entityCache, dumpDir)
  }

  private fun checkUnhandledCacheDump() {
    val dir = dumpDir
    if (dir.exists() && !dir.isDirectory) {
      throw IllegalStateException("${dir.absolutePath}不是文件夹")
    }
    if (dir.exists() && dir.isDirectory && !dir.list().isNullOrEmpty()) {
      throw UnhandledEntityCacheDumpException(dir)
    }
  }

  private fun cacheDump(cache: EntityCache<*, *>, outputDir: File) {
    val simpleName = cache.entityClass.simpleName
    try {
      if (!outputDir.exists()) {
        outputDir.mkdirs()
      }
      val content = cache.dump()
      val file = outputDir.resolve("$simpleName.json")
      file.writeText(content)
      logger.info { "[$simpleName]缓存数据保存文件成功: $file" }
    } catch (e: Exception) {
      logger.error(e) { "[$simpleName]缓存数据保存文件失败" }
    }
  }
}
