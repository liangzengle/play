package play.db.jdbc.mysql

import play.db.Repository
import play.db.jdbc.JdbcGuiceModule

class MysqlGuiceModule : JdbcGuiceModule() {

  override val vendor: String = "mysql"

  override fun configure() {
    super.configure()
    bind<Repository>().qualifiedWith(vendor).to<MysqlRepository>()
  }
}
