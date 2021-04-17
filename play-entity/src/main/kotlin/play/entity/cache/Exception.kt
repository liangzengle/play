package play.entity.cache

import java.io.File

class EntityExistsException internal constructor(entityClass: Class<*>, id: Any) :
  RuntimeException("${entityClass.simpleName}($id)已经存在")

class UnhandledCacheDumpException internal constructor(file: File) :
  RuntimeException(file.absolutePath)
