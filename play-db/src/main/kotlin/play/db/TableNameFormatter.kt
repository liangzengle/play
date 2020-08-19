package play.db

import play.Configuration
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

abstract class TableNameFormatter(dbConf: Configuration) {

  protected val postfixesToTrim: List<String> = dbConf.getStringList("table-name-formatter-trim-postfixes")

  abstract fun format(input: String): String

  protected fun trimPostfixes(input: String, postfixes: List<String>): String {
    for (i in postfixes.indices) {
      val postfix = postfixes[i]
      if (input.endsWith(postfix)) {
        return input.substring(0, input.length - postfix.length)
      }
    }
    return input
  }
}

@Singleton
class DefaultTableNameFormatter @Inject constructor(@Named("db") conf: Configuration) :
  TableNameFormatter(conf) {

  override fun format(input: String): String {
    return trimPostfixes(input, postfixesToTrim)
  }
}

@Singleton
class SnakeStyleTableNameFormatter @Inject constructor(@Named("db") conf: Configuration) :
  TableNameFormatter(conf) {

  override fun format(input: String): String {
    val name = trimPostfixes(input, postfixesToTrim)
    val b = StringBuilder(name.length + 4)
    for (i in name.indices) {
      val c = name[i]
      if (c.isUpperCase()) {
        if (i == 0) b.append(c.toLowerCase())
        else {
          b.append('_').append(c.toLowerCase())
        }
      } else {
        b.append(c)
      }
    }
    return b.toString()
  }
}
