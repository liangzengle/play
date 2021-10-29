package play.res

import com.google.common.collect.Collections2
import com.google.common.collect.Sets
import play.Log
import play.res.reader.ConfigReader
import play.res.reader.JsonResourceReader
import play.res.reader.Reader
import play.res.validation.validator.ResourceValidator
import play.util.collection.ConcurrentHashSet
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
import java.util.function.Consumer
import javax.annotation.concurrent.GuardedBy

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

  private var reloadListeners = Collections.synchronizedMap(WeakHashMap<ResourceReloadListener, Boolean>())

  private var resourceSets = emptyMap<Class<AbstractResource>, ResourceSet<AbstractResource>>()

  var version = "Unknown"
    private set

  private val cascadeResourceTypes = hashMapOf<Class<*>, MutableSet<Class<*>>>()

  private fun isInitialized() = resourceSets !== emptyMap<Class<*>, ResourceSet<AbstractResource>>()

  private fun <T : AbstractResource, R : ResourceSet<T>> getResourceSet(clazz: Class<T>): R {
    if (!isInitialized()) {
      throw IllegalStateException("${javaClass.simpleName}初始化未完成")
    }
    return resourceSets[clazz as Class<AbstractResource>] as? R
      ?: throw NoSuchElementException(clazz.simpleName + "Set")
  }

  @Synchronized
  fun init() {
    if (isInitialized()) throw IllegalStateException("has been initialized.")
    resourceClasses.filterDuplicatedBy { getReader(it).getURL(it).getOrThrow() }
      .also { if (it.isNotEmpty()) throw IllegalStateException("配置路径冲突: ${it.values}") }
    resourceSets = load(resourceClasses, true)
    initDelegatingResourceSets(resourceSets)
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
    val finalResourcesToLoad = classesToReload + classesToReload.asSequence()
      .flatMap { cascadeResourceTypes[it] ?: emptySet() }
      .map { it.unsafeCast<Class<out AbstractResource>>() }.toSet()
    val canNotReload = classesToReload.filterNot { isReloadable(it) }
    if (canNotReload.isNotEmpty()) {
      Log.warn { "以下配置类不允许重加载: $canNotReload" }
    }
    val reloaded = load(finalResourcesToLoad, setting.validateOnReload)
    for ((k, v) in reloaded) {
      if (!k.isAnnotationPresent(AllowRemoveOnReload::class.java)) {
        val prev = DelegatingResourceSet.getOrNull<AbstractResource>(k) ?: continue
        for (elem in prev.list()) {
          if (!v.contains(elem.id)) {
            Log.warn { "重加载时删除了配置: ${k.simpleName}(${elem.id})" }
          }
        }
      }
    }
    this.resourceSets = this.resourceSets + reloaded
    updateDelegatingResourceSets(reloaded)

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
    notifyReloadListeners(finalResourcesToLoad)
  }

  @Suppress("UnstableApiUsage")
  fun registerReloadListener(listener: ResourceReloadListener) {
    reloadListeners[listener] = true
  }

  @Suppress("UnstableApiUsage")
  fun registerReloadListeners(listeners: Collection<ResourceReloadListener>) {
    reloadListeners.putAll(Collections2.transform(listeners) { it to true })
  }

  private fun notifyReloadListeners(reloaded: Set<Class<out AbstractResource>>) {
    reloadListeners.keys.forEach(Consumer { listener ->
      try {
        listener.onResourceReloaded(reloaded)
      } catch (e: Exception) {
        Log.error(e) { e.message }
      }
    })
  }

  @GuardedBy("this")
  private fun load(
    classes: Collection<Class<out AbstractResource>>,
    validate: Boolean
  ): Map<Class<AbstractResource>, ResourceSet<AbstractResource>> {
    val resourceClassToResourceSet = read(classes)
    val errors = ConcurrentLinkedQueue<String>()
    val allResourceSets = this.resourceSets + resourceClassToResourceSet
    val resourceSetProvider = ResourceSetProvider(allResourceSets)
    val dependentMap = hashMapOf<Class<*>, Set<Class<*>>>()
    for ((k, v) in resourceClassToResourceSet) {
      val dependentResources = ConcurrentHashSet<Class<*>>()
      resourceSetProvider.dependentResources = dependentResources
      v.list().parallelStream().forEach { e ->
        e.initialize(resourceSetProvider, errors)
      }
      resourceSetProvider.dependentResources = null
      if (dependentResources.isNotEmpty()) {
        dependentMap[k] = dependentResources
      }
    }
    for ((k, set) in dependentMap) {
      for (v in set) {
        cascadeResourceTypes.computeIfAbsent(v) { Sets.newHashSetWithExpectedSize(1) }.add(k)
      }
    }
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors)
    }
    if (validate) {
      Log.info { "开始[配置验证]" }
      val validationErrors = ConstraintsValidator(allResourceSets).validate(classes)
      errors += validationErrors
      for (validator in validators) {
        validator.validate(resourceSetProvider, errors)
      }
      Log.info { "完成[配置验证]" }
    }
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors)
    }
    return resourceClassToResourceSet
  }

  private fun updateDelegatingResourceSets(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>) {
    DelegatingResourceSet.update(resourceSets)
  }

  private fun initDelegatingResourceSets(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>) {
    DelegatingResourceSet.init(resourceSets)
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

    val resourceDirs = getTopLevelResourceDirs(resourceClasses)
    for (resourceDir in resourceDirs) {
      watch(resourceDir)
      Log.info { "监听配置文件变化: ${resourceDir.absolutePath}" }
    }
  }

  private fun getTopLevelResourceDirs(resourceClasses: Iterable<Class<*>>): Set<File> {
    val dirPaths = resourceClasses
      .asSequence()
      .filter { ResourceHelper.isConfig(it) }
      .map { clazz ->
        val url = configReader.getURL(clazz).getOrThrow()
        Paths.get(url.toURI()).parent
      }
      .plusElement(Paths.get(resolver.rootPath))
      .toMutableSet()

    // 移除子目录
    while (true) {
      val prevSize = dirPaths.size
      val it = dirPaths.iterator()
      while (it.hasNext()) {
        val path = it.next()
        for (dirPath in dirPaths) {
          if (path.parent == dirPath) {
            it.remove()
            break
          }
        }
      }
      if (prevSize == dirPaths.size) {
        break
      }
    }
    return dirPaths
      .mapNotNull { path -> Result.runCatching { path.toFile() }.getOrNull() }
      .toSet()
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

  private fun read(classes: Collection<Class<out AbstractResource>>): Map<Class<AbstractResource>, ResourceSet<AbstractResource>> {
    return classes.parallelStream()
      .map {
        val clazz = it as Class<AbstractResource>
        val result = getReader(clazz).read(clazz)
        val resourceSet = ResourceHelper.createResourceSet(clazz, result)
        clazz to resourceSet
      }.toImmutableMap()
  }

  private fun updateVersion() {
    val versionFile = setting.versionFile
    if (versionFile.isEmpty()) {
      Log.debug { "无配置版本号" }
      return
    }
    resolver.resolve(versionFile)
      .mapCatching { it.readText() }
      .onSuccess {
        version = it
        Log.info { "配置版本号: $version" }
      }
      .onFailure { e ->
        Log.warn { "读取配置版本号文件失败: ${e.javaClass.name}: ${e.message}" }
      }
  }
}
