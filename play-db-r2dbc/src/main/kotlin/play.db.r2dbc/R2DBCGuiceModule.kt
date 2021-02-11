package play.db.r2dbc

import io.r2dbc.spi.ConnectionFactory
import play.Configuration
import play.inject.guice.GuiceModule

/**
 *
 * @author LiangZengle
 */
abstract class R2DBCGuiceModule : GuiceModule() {

  protected abstract val vendor: String

  override fun configure() {
    val vendorConf = ctx.conf.getConfiguration("db.$vendor")
    bind<Configuration>().qualifiedWith(vendor).toInstance(vendorConf)
    bind<DBConfig>().toInstance(toDBConfig(vendorConf))

    optionalBind<ConnectionFactory>().setDefault().toProvider(DefaultConnectionFactoryProvider::class.java)
  }

  protected open fun toDBConfig(conf: Configuration): DBConfig {
    return toDBConfig(conf, vendor)
  }

  private fun toDBConfig(conf: Configuration, protocol: String): DBConfig {
    val host = conf.getString("host")
    val port = conf.getInt("port")
    val db = conf.getString("db")
    val username = conf.getString("username")
    val password = conf.getString("password")
    return DBConfig(host, port, db, protocol, username, password)
  }
}
