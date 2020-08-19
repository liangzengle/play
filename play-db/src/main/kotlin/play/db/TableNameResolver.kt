package play.db

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableNameResolver @Inject constructor(private val tableNameFormatter: TableNameFormatter) {

  private val tableNameCache = ConcurrentHashMap<Class<*>, String>()

  fun resolve(clazz: Class<*>): String {
    val cached = tableNameCache[clazz]
    if (cached != null) {
      return cached
    }

    val tableName = clazz.getAnnotation(TableName::class.java)
    var name = tableName?.value ?: clazz.simpleName
    name = tableNameFormatter.format(name)
    tableNameCache[clazz] = name
    return name
  }
}
