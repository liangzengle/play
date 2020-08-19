package play.db.cache

import java.io.File

class EntityExistsException internal constructor(entityClass: Class<*>, id: Any) :
  RuntimeException("${entityClass.simpleName}($id)已经存在")

class EntityCacheInitializeException internal constructor(entityClass: Class<*>, cause: Throwable) :
  RuntimeException("实体缓存初始化失败: ${entityClass.simpleName}", cause)

class UnhandledCacheDumpException internal constructor(file: File) :
  RuntimeException(file.absolutePath)
