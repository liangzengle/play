package play.db

import java.util.concurrent.ConcurrentHashMap

class TableNameResolver constructor(
  private val postfixesToTrimBeforeFormat: List<String>,
  private val tableNameFormatter: TableNameFormatter
) {

  constructor(tableNameFormatter: TableNameFormatter) : this(emptyList(), tableNameFormatter)

  private val tableNameCache = ConcurrentHashMap<Class<*>, String>()

  fun resolve(clazz: Class<*>): String {
    val cached = tableNameCache[clazz]
    if (cached != null) {
      return cached
    }

    val tableName = clazz.getAnnotation(TableName::class.java)
    var name = tableName?.value ?: clazz.simpleName
    if (postfixesToTrimBeforeFormat.isNotEmpty()) {
      name = trimPostfixes(name, postfixesToTrimBeforeFormat)
    }
    name = tableNameFormatter.format(name)
    tableNameCache[clazz] = name // harmless contention
    return name
  }

  private fun trimPostfixes(input: String, postfixesToTrim: List<String>): String {
    for (i in postfixesToTrim.indices) {
      val postfix = postfixesToTrim[i]
      if (input.endsWith(postfix)) {
        return input.substring(0, input.length - postfix.length)
      }
    }
    return input
  }
}
