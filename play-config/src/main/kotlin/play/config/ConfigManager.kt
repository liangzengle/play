package play.config

import com.google.common.collect.ImmutableList
import play.ClassScanner
import play.Configuration
import play.Log
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.filterDuplicated
import play.util.collection.filterDuplicatedBy
import play.util.collection.toImmutableMap
import play.util.collection.toImmutableSet
import play.util.control.exists
import play.util.reflect.Reflect
import play.util.reflect.isAbstract
import play.util.reflect.isAssignableFrom
import play.util.scheduling.Scheduler
import play.util.unsafeCast
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.Comparator
import kotlin.NoSuchElementException

internal typealias AnyConfigSet = SuperConfigSet<Any, AbstractConfig, Any, ConfigExtension<AbstractConfig>>

@Suppress("UNCHECKED_CAST")
@Singleton
class ConfigManager @Inject constructor(
  @Named("config") conf: Configuration,
  private val resolver: ConfigResolver,
  private val configReader: ConfigReader,
  private val resourceReader: ResourceReader,
  private val classScanner: ClassScanner,
  injector: Injector,
  private val scheduler: Scheduler
) : PostConstruct {

  private val loadOnPostConstruct = conf.getBoolean("load-on-post-construct")
  private val validateOnReload = conf.getBoolean("validate-on-reload")
  private val versionFile = conf.getString("version-file")
  private val modificationDetectInterval = conf.getDuration("modification-detect-interval")

  private val listeners = injector.getInstancesOfType(ConfigRefreshListener::class.java).asSequence().map { o ->
    val eventType: Class<ConfigRefreshEvent> = if (isAssignableFrom<GenericConfigRefreshListener<*, *>>(o.javaClass)) {
      Reflect.getRawClass(
        Reflect.getTypeArg(
          o.javaClass.asSubclass(GenericConfigRefreshListener::class.java),
          GenericConfigRefreshListener::class.java,
          1
        )
      )
    } else {
      Reflect.getRawClass(
        Reflect.getTypeArg(
          o.javaClass,
          ConfigRefreshListener::class.java,
          0
        )
      )
    }
    eventType to o.unsafeCast<ConfigRefreshListener<ConfigRefreshEvent>>()
  }.toList()

  private val validators = injector.getInstancesOfType(ConfigValidator::class.java)

  private var configSets = emptyMap<Class<AbstractConfig>, AnyConfigSet>()

  private var configClasses: Set<Class<out AbstractConfig>> =
    classScanner.getConcreteSubTypesSequence(AbstractConfig::class.java)
      .filterNot { it.isAnnotationPresent(Ignore::class.java) }.toSet()

  var version = "Unspecified"
    private set

  private fun isInitialized() = configSets !== emptyMap<Class<AbstractConfig>, AnyConfigSet>()

  private fun isAutoReloadEnabled(): Boolean {
    return !modificationDetectInterval.isZero
  }

  fun <T : AbstractConfig, R : BasicConfigSet<T>> getConfigSet(clazz: Class<T>): R {
    if (!isInitialized()) {
      throw IllegalStateException("${javaClass.simpleName}初始化未完成")
    }
    return configSets[clazz as Class<AbstractConfig>] as? R
      ?: throw NoSuchElementException(clazz.simpleName + "Set")
  }

  override fun postConstruct() {
    if (loadOnPostConstruct) {
      load()
    }
  }

  @Synchronized
  fun load() {
    if (isInitialized()) throw IllegalStateException("Cannot load twice.")
    val classes = classScanner.getConcreteSubTypesSequence(AbstractConfig::class.java)
      .filter { isLoadable(it) }.map { it as Class<AbstractConfig> }.toImmutableSet()
    classes.filterDuplicatedBy { getReader(it).getURL(it).getOrThrow() }
      .also { if (it.isNotEmpty()) throw IllegalStateException("配置路径冲突: ${it.values}") }
    configSets = load(classes, true)
    updateDelegatedConfigSets(configSets)
    updateVersion()
    fireConfigEvent(ConfigLoadEvent(classes))
    if (isAutoReloadEnabled()) {
      setupAutoReload()
    }
  }

  fun reloadAll() {
    reload(configSets.keys)
  }

  fun reload(clazz: Class<out AbstractConfig>) {
    reload(setOf(clazz))
  }

  fun reload(classesToReload: Set<Class<out AbstractConfig>>) {
    val canNotReload = classesToReload.filterNot { isReloadable(it) }
    if (canNotReload.isNotEmpty()) {
      Log.warn { "以下配置类不允许重加载: $canNotReload" }
    }
    val reloaded = load(classesToReload, validateOnReload)
    updateDelegatedConfigSets(reloaded)

    fun getFileName(cls: Class<*>): String {
      return getReader(cls).getURL(cls).map { url ->
        val idx = url.path.lastIndexOf('/')
        if (idx != -1) url.path.substring(idx + 1)
        else url.path
      }.getOrDefault("")
    }
    for (cls in reloaded.keys) {
      Log.info { "配置重加载完成: ${cls.simpleName}(${getFileName(cls)})" }
    }
    updateVersion()
    fireConfigEvent(ConfigReloadEvent(classesToReload))
  }

  private fun fireConfigEvent(event: ConfigRefreshEvent): Boolean {
    var success = true
    val eventType = event.javaClass
    for ((listenedEventType, listener) in listeners) {
      if (listenedEventType.isAssignableFrom(eventType)
        && (listener !is GenericConfigRefreshListener<*, *> || event.contains(listener.configClass))
      ) {
        try {
          listener.onEvent(event)
        } catch (e: Exception) {
          success = false
          Log.error(e) { e.message }
        }
      }
    }
    return success
  }

  private fun load(
    classes: Collection<Class<out AbstractConfig>>,
    validate: Boolean
  ): Map<Class<AbstractConfig>, AnyConfigSet> {
    val configClassToConfigSet = read(classes)
    val errors = LinkedList<String>()
    configClassToConfigSet.values.forEach {
      it.list().forEach { e ->
        e.postInitialize(errors)
      }
    }
    if (errors.isNotEmpty()) {
      throw InvalidConfigException(errors)
    }
    if (validate) {
      val validationErrors = ConstraintsValidator(configClassToConfigSet).validate(classes)
      errors += validationErrors
    }
    val configSetManager = ConfigSetSupplier(this.configSets + configClassToConfigSet)
    validators.forEach {
      if (it !is GenericConfigValidator<*> || configClassToConfigSet.containsKey(it.configClass)) {
        it.validate(configSetManager, errors)
      }
    }
    if (errors.isNotEmpty()) {
      throw InvalidConfigException(errors.joinToString("\n", "\n", ""))
    }
    return configClassToConfigSet
  }

  private fun updateDelegatedConfigSets(configSets: Map<Class<AbstractConfig>, AnyConfigSet>) {
    configSets.forEach { (t, u) -> DelegatedConfigSet.get(t).updateDelegatee(u) }
  }

  private fun setupAutoReload() {
    val action: (Set<File>) -> Unit = { changeList ->
      val changeSet = changeList.asSequence().map { it.toURI().toURL() }.toSet()
      val classesToReload = configClasses.asSequence()
        .filter { clazz ->
          getReader(clazz).getURL(clazz).exists { changeSet.contains(it) }
        }.toSet()
      reload(classesToReload)
    }

    // 监听classpath中的配置文件
    configClasses.asSequence()
      .filter(::isResource)
      .map { clazz ->
        val url = configReader.getURL(clazz).getOrThrow()
        Paths.get(url.toURI()).parent.toFile()
      }
      .distinct()
      .forEach { dir ->
        ConfigDirectoryMonitor(dir, action).start(modificationDetectInterval, scheduler)
      }

    // 监听策划配置文件
    try {
      val dir = Paths.get(resolver.rootPath).toFile()
      ConfigDirectoryMonitor(dir, action).start(modificationDetectInterval, scheduler)
    } catch (e: UnsupportedOperationException) {
      Log.info { "启用配置自动重加载失败, 配置文件的路径不是文件路径: ${resolver.rootPath}" }
    }
  }

  private fun isResource(clazz: Class<*>): Boolean {
    return clazz.isAnnotationPresent(Resource::class.java)
  }

  private fun getReader(clazz: Class<*>): Reader {
    return if (isResource(clazz)) resourceReader else configReader
  }

  private fun isLoadable(clazz: Class<*>): Boolean {
    return !clazz.isAbstract() && !clazz.isAnnotationPresent(Ignore::class.java)
  }

  private fun isReloadable(clazz: Class<*>): Boolean {
    return isLoadable(clazz) && !clazz.isAnnotationPresent(NotReloadable::class.java)
  }

  private fun read(classes: Collection<Class<out AbstractConfig>>): Map<Class<AbstractConfig>, AnyConfigSet> {
    return classes.parallelStream()
      .map {
        val clazz = it as Class<AbstractConfig>
        val result = getReader(clazz).read(clazz)
        clazz to createConfigSet(clazz, result)
      }.toImmutableMap()
  }

  private fun updateVersion() {
    if (versionFile.isEmpty()) {
      Log.debug { "无配置版本号" }
      return
    }
    val maybeVersion = resolver.resolve(versionFile).map { it.readText() }
    maybeVersion.onSuccess {
      version = it
      Log.info { "配置版本号: $version" }
    }
    maybeVersion.onFailure { e ->
      Log.warn { "读取配置版本号文件失败: ${e.javaClass.name}: ${e.message}" }
    }
  }

  private fun createConfigSet(configClass: Class<AbstractConfig>, elems: List<AbstractConfig>): AnyConfigSet {
    val simpleName = configClass.simpleName
    try {
      val errors = LinkedList<String>()

      val isResource = configClass.isAnnotationPresent(Resource::class.java)
      val isSingleton = isResource || configClass.isAnnotationPresent(SingletonConfig::class.java)
      val notEmpty = isSingleton || configClass.isAnnotationPresent(NoneEmpty::class.java)
      if (notEmpty && elems.isEmpty()) {
        errors.add("表不能为空: $simpleName")
        throw InvalidConfigException(errors)
      }

      if (isSingleton && elems.size != 1) {
        errors.add("只能有1条配置: $simpleName")
      }

      val minId = configClass.getAnnotation(MinID::class.java)?.value ?: 1
      val invalidIdList = elems.asSequence().filter { it.id < minId }.map { it.id }.toList()
      if (invalidIdList.isNotEmpty()) {
        errors.add("ID必须大于[$minId]: $simpleName$invalidIdList")
      }

      val duplicatedIds = elems.asSequence().map { it.id }.filterDuplicated()
      if (duplicatedIds.isNotEmpty()) {
        errors.add("ID重复: $simpleName${duplicatedIds.values}")
      }

      val hasUniqueKey = UniqueKey::class.java.isAssignableFrom(configClass)
      if (hasUniqueKey) {
        val duplicatedKeys = elems.asSequence().map { (it as UniqueKey<Any>).key() }.filterDuplicated()
        if (duplicatedKeys.isNotEmpty()) {
          errors.add("Key重复: $simpleName${duplicatedKeys.values}")
        }
      }

      if (errors.isNotEmpty()) {
        throw InvalidConfigException(errors)
      }

      if (isSingleton) {
        return SingletonConfigSetImpl(elems.first())
      }

      if (elems.isEmpty()) {
        return ConfigSetImpl(configClass, ImmutableList.of())
      }

      val comparator = if (hasUniqueKey) {
        val keyComparator = (elems.first() as UniqueKey<Any>).keyComparator()
        Comparator<AbstractConfig> { o1, o2 ->
          o1 as UniqueKey<Any>
          o2 as UniqueKey<Any>
          keyComparator.compare(o1, o2)
        }
      } else {
        Comparator.comparingInt { it.id }
      }
      val array = elems.toTypedArray()
      Arrays.sort(array, comparator)
      return ConfigSetImpl(configClass, array.asList())
    } catch (e: InvalidConfigException) {
      throw e
    } catch (e: Exception) {
      throw IllegalStateException("ConfigSet[$simpleName]创建失败", e)
    }
  }
}

