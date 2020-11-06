package play.db.jdbc.mysql

import play.Configuration
import play.db.Repository
import play.db.jdbc.HikariDataSourceProvider
import play.db.jdbc.JdbcConfiguration
import play.inject.guice.GuiceModule
import javax.sql.DataSource

class MysqlGuiceModule : GuiceModule() {
  override fun configure() {
    bind<Repository>().qualifiedWith("mysql").to<MysqlRepository>()

    val mysqlConf = ctx.conf.getConfiguration("db.mysql")
    bind<Configuration>().qualifiedWith("mysql").toInstance(mysqlConf)
    bind<JdbcConfiguration>().toInstance(toJdbcConfiguration(mysqlConf))

    bind<DataSource>().qualifiedWith("hikari").toProvider(HikariDataSourceProvider::class.java)
    val jdbcDataSource = ctx.conf.getString("db.jdbc.data-source")
    bind<DataSource>().toBinding(binding(jdbcDataSource))
  }

  private fun toJdbcConfiguration(mysqlConf: Configuration): JdbcConfiguration {
    val host = mysqlConf.getString("host")
    val port = mysqlConf.getInt("port")
    val db = mysqlConf.getString("db")
    val b = StringBuilder()
    for ((k, v) in mysqlConf.getConfig("connection-properties").entrySet()) {
      if (b.isNotEmpty()) {
        b.append('&')
      }
      b.append(k).append('=').append(v.unwrapped())
    }
    val protocol = "jdbc:mysql"
    val queryString = b.toString()
    val username = mysqlConf.getString("username")
    val password = mysqlConf.getString("password")
    val driver = mysqlConf.getString("driver")
    return JdbcConfiguration(host, port, db, protocol, queryString, username, password, driver)
  }
}
