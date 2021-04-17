package play.db

abstract class TableNameFormatter {

  abstract fun format(input: String): String
}

class AsItIsFormatter : TableNameFormatter() {

  override fun format(input: String): String {
    return input
  }
}

class LowerUnderscoreFormatter : TableNameFormatter() {

  override fun format(input: String): String {
    val b = StringBuilder(input.length + 4)
    for (i in input.indices) {
      val c = input[i]
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
