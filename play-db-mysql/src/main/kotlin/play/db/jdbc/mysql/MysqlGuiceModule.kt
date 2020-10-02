package play.db.jdbc.mysql

import com.google.inject.Provides
import play.Configuration
import play.db.Repository
import play.db.jdbc.HikariDataSourceProvider
import play.db.jdbc.JdbcConfiguration
import play.inject.guice.GuiceModule
import javax.inject.Named
import javax.sql.DataSource

class MysqlGuiceModule : GuiceModule() {
  override fun configure() {
    bind<Repository>().qualifiedWith("mysql").to<MysqlRepository>()

    bind<Configuration>().qualifiedWith("mysql").toInstance(ctx.conf.getConfiguration("db.mysql"))

    bind<DataSource>().qualifiedWith("hikari").toProvider(HikariDataSourceProvider::class.java)
    val jdbcDataSource = ctx.conf.getString("db.jdbc.data-source")
    bind<DataSource>().toBinding(binding(jdbcDataSource))
  }

  @Provides
  fun jdbcConf(@Named("mysql") cfg: Configuration): JdbcConfiguration {
    val host = cfg.getString("host")
    val port = cfg.getInt("port")
    val db = cfg.getString("db")
    val b = StringBuilder()
    for ((k, v) in cfg.getConfig("connection-properties").entrySet()) {
      if (b.isNotEmpty()) {
        b.append('&')
      }
      b.append(k).append('=').append(v.unwrapped())
    }
    val protocol = "jdbc:mysql"
    val queryString = b.toString()
    val username = cfg.getString("username")
    val password = cfg.getString("password")
    val driver = cfg.getString("driver")
    return JdbcConfiguration(host, port, db, protocol, queryString, username, password, driver)
  }
}
