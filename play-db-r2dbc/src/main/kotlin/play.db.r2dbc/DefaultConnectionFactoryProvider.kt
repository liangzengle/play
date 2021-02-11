package play.db.r2dbc

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import java.time.Duration
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
class DefaultConnectionFactoryProvider @Inject constructor(private val dbConfig: DBConfig) :
  Provider<ConnectionFactory> {

  private val factory: ConnectionFactory by lazy {
    val options = ConnectionFactoryOptions.builder()
      .option(ConnectionFactoryOptions.DRIVER, "connection-pool")
      .option(ConnectionFactoryOptions.PROTOCOL, dbConfig.protocol)
      .option(ConnectionFactoryOptions.HOST, dbConfig.host)
      .option(ConnectionFactoryOptions.PORT, dbConfig.port) // optional, default 3306
      .option(ConnectionFactoryOptions.USER, dbConfig.username)
      .option(
        ConnectionFactoryOptions.PASSWORD,
        dbConfig.password
      ) // optional, default null, null means has no password
      .option(
        ConnectionFactoryOptions.DATABASE,
        dbConfig.dbName
      ) // optional, default null, null means not specifying the database
      .option(
        ConnectionFactoryOptions.CONNECT_TIMEOUT,
        Duration.ofSeconds(3)
      ) // optional, default null, null means no timeout
      .option(Option.valueOf("useServerPrepareStatement"), true) // optional, default false
      .option(Option.valueOf("tcpKeepAlive"), true) // optional, default false
      .option(Option.valueOf("tcpNoDelay"), true) // optional, default false
      .option(Option.valueOf("autodetectExtensions"), false) // optional, default false
      .option(Option.valueOf("maxSize"), 4)
      .build()
    ConnectionFactories.get(options)
  }

  override fun get(): ConnectionFactory = factory
}
