package play.config

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.typesafe.config.Config
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.annotation.concurrent.GuardedBy
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.setOf
import play.Log
import play.inject.PlayInjector
import play.inject.PostConstruct
import play.scheduling.Scheduler
import play.util.collection.*
import play.util.control.exists
import play.util.isAbstract
import play.util.reflect.ClassScanner
import play.util.unsafeCast
import play.util.unsafeLazy

internal typealias AnyConfigSet = SuperConfigSet<Any, AbstractConfig, Any, ConfigExtension<AbstractConfig>>

@Suppress("UNCHECKED_CAST")
class ConfigManager @Inject constructor(
  @Named("config") conf: Config,
  private val resolver: ConfigResolver,
  private val configReader: ConfigReader,
  private val resourceReader: ResourceReader,
  private val injector: PlayInjector,
  private val validators: List<ConfigValidator>,
  private val scheduler: Scheduler,
  classScanner: ClassScanner
) : PostConstruct {

  private val loadOnPostConstruct = conf.getBoolean("load-on-post-construct")
  private val validateOnReload = conf.getBoolean("validate-on-reload")
  private val versionFile = conf.getString("version-file")
  private val modificationDetectInterval = conf.getDuration("modification-detect-interval")

  private val reloadListeners by unsafeLazy { injector.getInstancesOfType(ConfigReloadListener::class.java) }

  private var configSets = emptyMap<Class<AbstractConfig>, AnyConfigSet>()

  private val configClasses: Set<Class<out AbstractConfig>> =
    classScanner.getOrdinarySubTypesSequence(AbstractConfig::class.java)
      .filterNot { it.isAnnotationPresent(Ignore::class.java) }
      .asSequence()
      .toImmutableSet()

  var version = "Unspecified"
    private set

  private val cascadeConfigTypes = hashMapOf<Class<*>, MutableSet<Class<*>>>()

  private fun isInitialized() = configSets !== emptyMap<Class<*>, AnyConfigSet>()

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
    configClasses.filterDuplicatedBy { getReader(it).getURL(it).getOrThrow() }
      .also { if (it.isNotEmpty()) throw IllegalStateException("配置路径冲突: ${it.values}") }
    configSets = load(configClasses, true)
    updateDelegatedConfigSets(configSets)
    updateVersion()
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

  fun reload(demandConfigsToReload: Set<Class<out AbstractConfig>>) {
    val finalConfigsToLoad = demandConfigsToReload + demandConfigsToReload.asSequence()
      .flatMap { cascadeConfigTypes[it] ?: emptySet() }
      .map { it.unsafeCast<Class<out AbstractConfig>>() }.toSet()
    val canNotReload = demandConfigsToReload.filterNot { isReloadable(it) }
    if (canNotReload.isNotEmpty()) {
      Log.warn { "以下配置类不允许重加载: $canNotReload" }
    }
    val reloaded = load(finalConfigsToLoad, validateOnReload)
    updateDelegatedConfigSets(reloaded)

    fun getFileName(cls: Class<*>): String {
      return getReader(cls).getURL(cls).map { url ->
        val idx = url.path.lastIndexOf('/')
        if (idx != -1) url.path.substring(idx + 1)
        else url.path
      }.getOrDefault("")
    }
    for (cls in reloaded.keys) {
      val isCascade = !demandConfigsToReload.contains(cls)
      Log.info { "配置重加载完成: ${cls.simpleName}(${getFileName(cls)})${if (isCascade) " (关联的更新)" else ""}" }
    }
    updateVersion()
    notifyReloadListeners(finalConfigsToLoad)
  }

  private fun notifyReloadListeners(reloaded: Set<Class<out AbstractConfig>>) {
    for (listener in reloadListeners) {
      try {
        listener.onConfigReloaded(reloaded)
      } catch (e: Exception) {
        Log.error(e) { e.message }
      }
    }
  }

  @GuardedBy("this")
  private fun load(
    classes: Collection<Class<out AbstractConfig>>,
    validate: Boolean
  ): Map<Class<AbstractConfig>, AnyConfigSet> {
    val configClassToConfigSet = read(classes)
    val errors = ConcurrentLinkedQueue<String>()
    val allConfigSets = this.configSets + configClassToConfigSet
    val configSetSupplier = ConfigSetSupplier(allConfigSets)
    val dependentMap = hashMapOf<Class<*>, Set<Class<*>>>()
    configClassToConfigSet.values.parallelStream().forEach {
      val dependentConfigs = ConcurrentHashSet<Class<*>>()
      configSetSupplier.dependentConfigs = dependentConfigs
      it.list().parallelStream().forEach { e ->
        e.postInitialize(configSetSupplier, errors)
      }
      configSetSupplier.dependentConfigs = null
      if (dependentConfigs.isNotEmpty()) {
        dependentMap[it.firstOrThrow().javaClass] = dependentConfigs
      }
    }
    for ((k, set) in dependentMap) {
      for (v in set) {
        cascadeConfigTypes.computeIfAbsent(v) { Sets.newHashSetWithExpectedSize(1) }.add(k)
      }
    }
    if (errors.isNotEmpty()) {
      throw InvalidConfigException(errors)
    }
    if (validate) {
      val validationErrors = ConstraintsValidator(allConfigSets).validate(classes)
      errors += validationErrors
    }
    validators.forEach {
      if (it !is GenericConfigValidator<*> || configClassToConfigSet.containsKey(it.configClass)) {
        it.validate(configSetSupplier, errors)
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
    fun reloadChangedFiles(changeList: Set<File>) {
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
        ConfigDirectoryMonitor(dir, ::reloadChangedFiles).start(modificationDetectInterval, scheduler)
      }

    // 监听策划配置文件
    try {
      val dir = Paths.get(resolver.rootPath).toFile()
      ConfigDirectoryMonitor(dir, ::reloadChangedFiles).start(modificationDetectInterval, scheduler)
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
