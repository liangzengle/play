package play.db.jdbc

data class JdbcConfiguration(
  @JvmField
  val host: String,
  @JvmField
  val port: Int,
  @JvmField
  val db: String,
  @JvmField
  val protocol: String,
  @JvmField
  val queryString: String,
  @JvmField
  val username: String,
  @JvmField
  val password: String,
  @JvmField
  val driver: String
) {

  fun getUrl(): String {
    val b = StringBuilder(256)
      .append(protocol)
      .append("://")
      .append(host).append(':')
      .append(port)
      .append('/')
      .append(db)
    if (queryString.isNotEmpty()) {
      b.append('?').append(queryString)
    }
    return b.toString()
  }

  fun getUrlNoDB(): String {
    val b = StringBuilder(256)
      .append(protocol)
      .append("://")
      .append(host).append(':')
      .append(port)
    if (queryString.isNotEmpty()) {
      b.append('?').append(queryString)
    }
    return b.toString()
  }
}
