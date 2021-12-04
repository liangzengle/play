package play.db

import org.jctools.maps.NonBlockingHashMap

class TableNameResolver constructor(
  private val postfixesToTrimBeforeFormat: List<String>,
  private val tableNameFormatter: TableNameFormatter
) {

  constructor(tableNameFormatter: TableNameFormatter) : this(emptyList(), tableNameFormatter)

  private val tableNameCache = NonBlockingHashMap<Class<*>, String>()

  fun resolve(clazz: Class<*>): String {
    val cached = tableNameCache[clazz]
    if (cached != null) {
      return cached
    }

    val tableName = clazz.getAnnotation(TableName::class.java)
    var name = clazz.simpleName
    if (tableName == null) {
      if (postfixesToTrimBeforeFormat.isNotEmpty()) {
        name = trimPostfixes(clazz.simpleName, postfixesToTrimBeforeFormat)
      }
      name = tableNameFormatter.format(name)
    } else {
      name = tableName.value
    }
    tableNameCache[clazz] = name
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
