package play.db

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.Configuration
import play.db.memory.MemoryRepository
import play.inject.guice.GuiceModule
import play.util.reflect.Reflect

@AutoService(Module::class)
class DbGuiceModule : GuiceModule() {

  @Suppress("UNCHECKED_CAST")
  override fun configure() {
    super.configure()

    val dbConf = ctx.conf.getConfiguration("db")
    bind<Configuration>().qualifiedWith("db").toInstance(dbConf)

    // table name formatter
    dbConf.getConfig("table-name-formatters").entrySet().forEach {
      val name = it.key
      val className = it.value.unwrapped().toString()
      bind<TableNameFormatter>().qualifiedWith(name).to(Class.forName(className) as Class<out TableNameFormatter>)
    }
    val tableNameFormatter = dbConf.getString("table-name-formatter")
    bind<TableNameFormatter>().toBinding(binding(tableNameFormatter))

    // repository
    val repository = ctx.conf.getString("db.repository")
    bind<Repository>().qualifiedWith("memory").to<MemoryRepository>()
    val dbProductModuleName = "product-modules.$repository"
    if (dbConf.hasPath(dbProductModuleName)) {
      val module = Reflect.createInstance<Module>(dbConf.getString(dbProductModuleName))
      if (module is GuiceModule) module.initContext(ctx)
      binder().install(module)
    }
    bind<Repository>().toBinding(binding(repository))

    bind<DbExecutor>().toProvider(DbExecutorProvider::class.java)

    bind<PersistService>().to<Repository>()
    bind<QueryService>().to<Repository>()
  }


}
