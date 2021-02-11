package play.db.r2dbc

/**
 *
 * @author LiangZengle
 */
data class DBConfig(
  val host: String,
  val port: Int,
  val dbName: String,
  val protocol: String,
  val username: String,
  val password: String
) {

  fun getUrlNoDB(): String {
    val b = StringBuilder(64)
      .append("jdbc:")
      .append(protocol)
      .append("://")
      .append(host).append(':')
      .append(port)
    return b.toString()
  }
}
