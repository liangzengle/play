package play.db.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import play.Configuration
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class HikariDataSourceProvider @Inject constructor(conf: JdbcConfiguration, @Named("db") dbConf: Configuration) :
  Provider<DataSource> {

  private val ds: DataSource by lazy {
    val cfg = HikariConfig()
    cfg.jdbcUrl = conf.getUrl()
    cfg.username = conf.username
    cfg.password = conf.password
    cfg.driverClassName = conf.driver
    cfg.maximumPoolSize = dbConf.getThreadNum("thread-pool-size")
    cfg.addDataSourceProperty("cachePrepStmts", "true")
    cfg.addDataSourceProperty("prepStmtCacheSize", "64")
    cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    cfg.addDataSourceProperty("useServerPrepStmts", "true")

    if (dbConf.hasPath("jdbc.hikari")) {
      dbConf.getConfig("jdbc.hikari").entrySet().forEach {
        val name = it.key
        val value = it.value.unwrapped().toString()
        cfg.addDataSourceProperty(name, value)
      }
    }

    HikariDataSource(cfg)
  }

  override fun get(): DataSource = ds
}
