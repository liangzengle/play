package play.db.jdbc

import play.Configuration
import play.inject.guice.GuiceModule
import javax.sql.DataSource

/**
 * JDBC数据库配置
 *
 * @author LiangZengle
 */
abstract class JdbcGuiceModule : GuiceModule() {

  /**
   * db vendor
   */
  protected abstract val vendor: String

  override fun configure() {
    val vendorConf = ctx.conf.getConfiguration("db.$vendor")
    bind<Configuration>().qualifiedWith(vendor).toInstance(vendorConf)
    bind<JdbcConfiguration>().toInstance(toJdbcConfiguration(vendorConf))

    bind<DataSource>().qualifiedWith("hikari").toProvider(HikariDataSourceProvider::class.java)
    val jdbcDataSource = ctx.conf.getString("db.jdbc.data-source")
    bind<DataSource>().toBinding(binding(jdbcDataSource))
  }

  protected open fun toJdbcConfiguration(conf: Configuration): JdbcConfiguration {
    return toJdbcConfiguration(conf, vendor)
  }

  private fun toJdbcConfiguration(conf: Configuration, protocol: String): JdbcConfiguration {
    val host = conf.getString("host")
    val port = conf.getInt("port")
    val db = conf.getString("db")
    val b = StringBuilder()
    for ((k, v) in conf.getConfig("connection-properties").entrySet()) {
      if (b.isNotEmpty()) {
        b.append('&')
      }
      b.append(k).append('=').append(v.unwrapped())
    }
    val queryString = b.toString()
    val username = conf.getString("username")
    val password = conf.getString("password")
    val driver = conf.getString("driver")
    return JdbcConfiguration(host, port, db, protocol, queryString, username, password, driver)
  }
}
