package play.res

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import play.Log
import play.util.collection.ConcurrentHashSet
import play.util.collection.filterDuplicated
import play.util.collection.filterDuplicatedBy
import play.util.collection.toImmutableMap
import play.util.control.exists
import play.util.createInstance
import play.util.io.FileMonitor
import play.util.isAbstract
import play.util.reflect.ClassScanner
import play.util.reflect.isAnnotationPresent
import play.util.unsafeCast
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.annotation.concurrent.GuardedBy

internal typealias AnyResourceSet = SuperResourceSet<Any, AbstractResource, Any, ResourceExtension<AbstractResource>>

@Suppress("UNCHECKED_CAST")
class ResourceManager(
  private val setting: Setting,
  private val resolver: ResourceUrlResolver,
  private val resourceReader: Reader,
  private val configReader: Reader,
  private val resourceClasses: List<Class<out AbstractResource>>,
  private val validators: List<ResourceValidator>
) {

  data class Setting(val validateOnReload: Boolean, val versionFile: String, val autoReload: Boolean)

  companion object {
    @JvmName("create")
    operator fun invoke(path: String, classScanner: ClassScanner): ResourceManager {
      val urlResolver = ResourceUrlResolver.forPath(path)
      val resourceReader = JsonResourceReader(urlResolver)
      val configReader = ConfigReader()
      val setting = Setting(true, "version.txt", true)
      val resourceClasses = classScanner
        .getOrdinarySubclassInfoList(AbstractResource::class.java)
        .filter { !it.isAnnotationPresent(Ignore::class.java) }
        .loadClasses(AbstractResource::class.java)
      val validators = classScanner.getOrdinarySubclasses(ResourceValidator::class.java).map { it.createInstance() }
      return ResourceManager(setting, urlResolver, resourceReader, configReader, resourceClasses, validators)
    }
  }

  private var reloadListeners = emptyList<ResourceReloadListener>()

  private var resourceSets = emptyMap<Class<AbstractResource>, AnyResourceSet>()

  var version = "Unspecified"
    private set

  private val cascadeResourceTypes = hashMapOf<Class<*>, MutableSet<Class<*>>>()

  private fun isInitialized() = resourceSets !== emptyMap<Class<*>, AnyResourceSet>()

  fun <T : AbstractResource, R : BasicResourceSet<T>> getConfigSet(clazz: Class<T>): R {
    if (!isInitialized()) {
      throw IllegalStateException("${javaClass.simpleName}初始化未完成")
    }
    return resourceSets[clazz as Class<AbstractResource>] as? R
      ?: throw NoSuchElementException(clazz.simpleName + "Set")
  }

  @Synchronized
  fun load() {
    if (isInitialized()) throw IllegalStateException("Cannot load twice.")
    resourceClasses.filterDuplicatedBy { getReader(it).getURL(it).getOrThrow() }
      .also { if (it.isNotEmpty()) throw IllegalStateException("配置路径冲突: ${it.values}") }
    resourceSets = load(resourceClasses, true)
    updateDelegatedConfigSets(resourceSets)
    updateVersion()
    if (setting.autoReload) {
      setupAutoReload()
    }
  }

  fun reloadAll() {
    reload(resourceSets.keys)
  }

  fun reload(clazz: Class<out AbstractResource>) {
    reload(setOf(clazz))
  }

  @Synchronized
  fun reload(classesToReload: Set<Class<out AbstractResource>>) {
    val finalConfigsToLoad = classesToReload + classesToReload.asSequence()
      .flatMap { cascadeResourceTypes[it] ?: emptySet() }
      .map { it.unsafeCast<Class<out AbstractResource>>() }.toSet()
    val canNotReload = classesToReload.filterNot { isReloadable(it) }
    if (canNotReload.isNotEmpty()) {
      Log.warn { "以下配置类不允许重加载: $canNotReload" }
    }
    val reloaded = load(finalConfigsToLoad, setting.validateOnReload)
    for ((k, v) in reloaded) {
      val prev = DelegatedResourceSet.getOrNull(k)
      if (prev != null && !k.isAnnotationPresent(AllowRemoveOnReload::class.java)) {
        for (elem in prev.list()) {
          if (!v.contains(elem.id)) {
            Log.warn { "重加载时删除了配置: ${k.simpleName}(${elem.id})" }
          }
        }
      }
    }
    updateDelegatedConfigSets(reloaded)

    fun getFileName(cls: Class<*>): String {
      return getReader(cls).getURL(cls).map { url ->
        val idx = url.path.lastIndexOf('/')
        if (idx != -1) url.path.substring(idx + 1)
        else url.path
      }.getOrDefault("")
    }
    for (cls in reloaded.keys) {
      val isCascade = !classesToReload.contains(cls)
      Log.info { "配置重加载完成: ${cls.simpleName}(${getFileName(cls)})${if (isCascade) " (关联的更新)" else ""}" }
    }
    updateVersion()
    notifyReloadListeners(finalConfigsToLoad)
  }

  @Suppress("UnstableApiUsage")
  @Synchronized
  fun addReloadListener(listener: ResourceReloadListener) {
    reloadListeners =
      ImmutableList.builderWithExpectedSize<ResourceReloadListener>(reloadListeners.size + 1)
        .addAll(reloadListeners)
        .add(listener)
        .build()
  }

  @Suppress("UnstableApiUsage")
  @Synchronized
  fun addReloadListeners(listeners: Collection<ResourceReloadListener>) {
    reloadListeners =
      ImmutableList.builderWithExpectedSize<ResourceReloadListener>(reloadListeners.size + listeners.size)
        .addAll(reloadListeners)
        .addAll(listeners)
        .build()
  }

  private fun notifyReloadListeners(reloaded: Set<Class<out AbstractResource>>) {
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
    classes: Collection<Class<out AbstractResource>>,
    validate: Boolean
  ): Map<Class<AbstractResource>, AnyResourceSet> {
    val configClassToConfigSet = read(classes)
    val errors = ConcurrentLinkedQueue<String>()
    val allConfigSets = this.resourceSets + configClassToConfigSet
    val resourceSetSupplier = ResourceSetSupplier(allConfigSets)
    val dependentMap = hashMapOf<Class<*>, Set<Class<*>>>()
    configClassToConfigSet.values.parallelStream().forEach {
      val dependentConfigs = ConcurrentHashSet<Class<*>>()
      resourceSetSupplier.dependentConfigs = dependentConfigs
      it.list().parallelStream().forEach { e ->
        e.postInitialize(resourceSetSupplier, errors)
      }
      resourceSetSupplier.dependentConfigs = null
      if (dependentConfigs.isNotEmpty()) {
        dependentMap[it.firstOrThrow().javaClass] = dependentConfigs
      }
    }
    for ((k, set) in dependentMap) {
      for (v in set) {
        cascadeResourceTypes.computeIfAbsent(v) { Sets.newHashSetWithExpectedSize(1) }.add(k)
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
      if (it !is GenericResourceValidator<*> || configClassToConfigSet.containsKey(it.configClass)) {
        it.validate(resourceSetSupplier, errors)
      }
    }
    if (errors.isNotEmpty()) {
      throw InvalidConfigException(errors.joinToString("\n", "\n", ""))
    }
    return configClassToConfigSet
  }

  private fun updateDelegatedConfigSets(configSets: Map<Class<AbstractResource>, AnyResourceSet>) {
    configSets.forEach { (t, u) -> DelegatedResourceSet.createOrUpdate(t, u) }
  }

  private fun setupAutoReload() {
    fun reloadChangedFiles(changeList: Set<File>) {
      val changeSet = changeList.asSequence().map { it.toURI().toURL() }.toSet()
      val classesToReload = resourceClasses.asSequence()
        .filter { clazz ->
          getReader(clazz).getURL(clazz).exists { changeSet.contains(it) }
        }.toSet()
      reload(classesToReload)
    }

    fun watch(dir: File) {
      FileMonitor.aggregatedBuilder()
        .watchFileOrDir(dir)
        .watchCreate()
        .watchModify()
        .onFileChange(::reloadChangedFiles)
        .build()
        .start()
    }

    // 监听classpath中的配置文件
    val resourceDirs = resourceClasses.asSequence()
      .filter { ResourceHelper.isConfig(it) }
      .map { clazz ->
        val url = resourceReader.getURL(clazz).getOrThrow()
        Paths.get(url.toURI()).parent
      }
      .toList()
    val topResourceDir = resourceDirs.minByOrNull { it.nameCount }
    if (topResourceDir != null) {
      for (resourceDir in resourceDirs) {
        check(resourceDir.startsWith(topResourceDir)) { "目录不一致: $topResourceDir $resourceDir" }
      }
      watch(topResourceDir.toFile())
    }

    // 监听配置文件变化
    try {
      val dir = Paths.get(resolver.rootPath).toFile()
      watch(dir)
    } catch (e: UnsupportedOperationException) {
      Log.info { "启用配置自动重加载失败, 配置文件的路径不是文件路径: ${resolver.rootPath}" }
    }
  }

  private fun getReader(clazz: Class<*>): Reader {
    return if (ResourceHelper.isConfig(clazz)) configReader else resourceReader
  }

  private fun isLoadable(clazz: Class<*>): Boolean {
    return !clazz.isAbstract() && !clazz.isAnnotationPresent(Ignore::class.java)
  }

  private fun isReloadable(clazz: Class<*>): Boolean {
    return isLoadable(clazz) && !clazz.isAnnotationPresent(NotReloadable::class.java)
  }

  private fun read(classes: Collection<Class<out AbstractResource>>): Map<Class<AbstractResource>, AnyResourceSet> {
    return classes.parallelStream()
      .map {
        val clazz = it as Class<AbstractResource>
        val result = getReader(clazz).read(clazz)
        clazz to createResourceSet(clazz, result)
      }.toImmutableMap()
  }

  private fun updateVersion() {
    val versionFile = setting.versionFile
    if (versionFile.isEmpty()) {
      Log.debug { "无配置版本号" }
      return
    }
    val maybeVersion = resolver.resolve(versionFile).mapCatching { it.readText() }
    maybeVersion.onSuccess {
      version = it
      Log.info { "配置版本号: $version" }
    }
    maybeVersion.onFailure { e ->
      Log.warn { "读取配置版本号文件失败: ${e.javaClass.name}: ${e.message}" }
    }
  }

  private fun createResourceSet(resourceClass: Class<AbstractResource>, elems: List<AbstractResource>): AnyResourceSet {
    val simpleName = resourceClass.simpleName
    try {
      val errors = LinkedList<String>()

      val isSingleton = ResourceHelper.isSingletonResource(resourceClass)
      val notEmpty = isSingleton || resourceClass.isAnnotationPresent(NonEmpty::class.java)
      if (notEmpty && elems.isEmpty()) {
        errors.add("表不能为空: $simpleName")
        throw InvalidConfigException(errors)
      }

      if (isSingleton && elems.size != 1) {
        errors.add("只能有1条配置: $simpleName")
      }

      val minId = resourceClass.getAnnotation(MinID::class.java)?.value ?: 1
      val invalidIdList = elems.asSequence().filter { it.id < minId }.map { it.id }.toList()
      if (invalidIdList.isNotEmpty()) {
        errors.add("ID必须大于[$minId]: $simpleName$invalidIdList")
      }

      val duplicatedIds = elems.asSequence().map { it.id }.filterDuplicated()
      if (duplicatedIds.isNotEmpty()) {
        errors.add("ID重复: $simpleName${duplicatedIds.values}")
      }

      val hasUniqueKey = UniqueKey::class.java.isAssignableFrom(resourceClass)
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
        return SingletonResourceSetImpl(elems.first())
      }

      if (elems.isEmpty()) {
        return ResourceSetImpl(resourceClass, ImmutableList.of())
      }

      val comparator = if (hasUniqueKey) {
        val keyComparator = (elems.first() as UniqueKey<Any>).keyComparator()
        Comparator<AbstractResource> { o1, o2 ->
          o1 as UniqueKey<Any>
          o2 as UniqueKey<Any>
          keyComparator.compare(o1, o2)
        }
      } else {
        Comparator.comparingInt { it.id }
      }
      val array = elems.toTypedArray()
      Arrays.sort(array, comparator)
      return ResourceSetImpl(resourceClass, array.asList())
    } catch (e: InvalidConfigException) {
      throw e
    } catch (e: Exception) {
      throw IllegalStateException("ConfigSet[$simpleName]创建失败", e)
    }
  }
}
