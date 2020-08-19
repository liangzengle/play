package play.db.jdbc

import io.vavr.control.Option

data class JdbcConfiguration(
  val url: String,
  val db: String,
  val username: String,
  val password: String,
  val parameters: Option<String>,
  val driver: Option<String>
)
