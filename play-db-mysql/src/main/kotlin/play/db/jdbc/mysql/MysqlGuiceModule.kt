package play.db.jdbc.mysql

import com.google.inject.Provides
import play.db.Repository
import play.db.jdbc.HikariDataSourceProvider
import play.db.jdbc.JdbcConfiguration
import play.inject.guice.GuiceModule
import javax.sql.DataSource

class MysqlGuiceModule : GuiceModule() {
  override fun configure() {
    bind<Repository>().qualifiedWith("mysql").to<MysqlRepository>()

    bind<DataSource>().qualifiedWith("hikari").toProvider(HikariDataSourceProvider::class.java)
    val jdbcDataSource = ctx.conf.getString("db.jdbc.data-source")
    bind<DataSource>().toBinding(binding(jdbcDataSource))
  }

  @Provides
  fun jdbcConf() = ctx.conf.getConfiguration("db.mysql").to<JdbcConfiguration>()
}
