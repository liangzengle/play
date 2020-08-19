package play.db.mongo

import com.mongodb.MongoClientSettings
import play.Configuration
import play.db.Repository
import play.inject.guice.GuiceModule
import play.util.reflect.classOf

class MongoGuiceModule : GuiceModule() {
  override fun configure() {
    val conf = ctx.conf.getConfiguration("db.mongodb")
    bind<Configuration>().qualifiedWith("mongodb").toInstance(conf)
    bind<Repository>().qualifiedWith("mongodb").to<MongoDBRespository>()
    optionalBind<MongoClientSettings.Builder>().setDefault().toProvider(classOf<MongoClientSettingBuilderProvider>())
    bind<MongoClientSettings>().toProvider(classOf<MongoClientSettingProvider>())
  }
}
