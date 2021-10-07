package play.res

import play.util.ClassUtil
import play.util.collection.filterDuplicated
import play.util.isAssignableFrom
import play.util.reflect.Reflect
import play.util.unsafeCast
import java.util.*

internal object ResourceHelper {
  private val idComparator = Comparator.comparingInt<AbstractResource> { it.id }
  private val keyComparator =
    Comparator.comparing<AbstractResource, Comparable<Comparable<*>>> {
      it.unsafeCast<UniqueKey<*>>().key().unsafeCast()
    }


  fun isSingletonResource(clazz: Class<*>): Boolean {
    return clazz.isAnnotationPresent(SingletonResource::class.java) || isConfig(clazz)
  }

  fun isConfig(clazz: Class<*>): Boolean {
    return isAssignableFrom<AbstractConfig>(clazz)
  }

  @Suppress("UNCHECKED_CAST")
  fun createResourceSet(
    resourceClass: Class<AbstractResource>,
    elems: List<AbstractResource>
  ): ResourceSet<AbstractResource> {
    val simpleName = resourceClass.simpleName
    try {
      val errors = LinkedList<String>()

      val isSingleton = isSingletonResource(resourceClass)
      val notEmpty = isSingleton || resourceClass.isAnnotationPresent(NonEmpty::class.java)
      if (notEmpty && elems.isEmpty()) {
        errors.add("配置不能为空: $simpleName")
        throw InvalidResourceException(errors)
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
        // TODO
        val duplicatedKeys = elems.asSequence().map { (it as UniqueKey<*>).key() }.filterDuplicated()
        if (duplicatedKeys.isNotEmpty()) {
          errors.add("Key重复: $simpleName${duplicatedKeys.values}")
        }
        val uniqueKeyType: Class<Any> = Reflect.getRawClassOfTypeArg(
          resourceClass.asSubclass(UniqueKey::class.java),
          UniqueKey::class.java,
          0
        )
        if (ClassUtil.getPrimitiveType(uniqueKeyType) == Int::class.java) {
          errors.add("UniqueKey的类型不能为Int: $simpleName")
        }
      }

      val hasGroupUniqueKey = GroupedUniqueKey::class.java.isAssignableFrom(resourceClass)
      if (hasGroupUniqueKey) {
        elems.asSequence().map { it.unsafeCast<GroupedUniqueKey<Any, *>>() }.groupBy { it.groupBy() }
          .forEach { (groupId, group) ->
            val treeSet = TreeSet<Comparable<*>>()
            val duplicated = TreeSet<Comparable<*>>()
            for (o in group) {
              val key = o.keyInGroup()
              if (!treeSet.add(key)) {
                duplicated.add(key)
              }
            }
            if (duplicated.isNotEmpty()) {
              errors.add("组内唯一key重复: resource=${simpleName}, group=$groupId, duplicatedKeys=$duplicated")
            }
          }
      }

      if (errors.isNotEmpty()) {
        throw InvalidResourceException(errors)
      }

      if (isSingleton) {
        return SingletonResourceSetImpl(elems.first())
      }

      if (elems.isEmpty()) {
        return EmptyResourceSet.of()
      }

      val comparator = if (hasUniqueKey) keyComparator else idComparator
      val array = elems.toTypedArray()
      Arrays.sort(array, comparator)
      return ResourceSetImpl<Comparable<*>, AbstractResource, Any, ResourceExtension<AbstractResource>>(
        resourceClass,
        array.asList()
      )
    } catch (e: InvalidResourceException) {
      throw e
    } catch (e: Exception) {
      throw IllegalStateException("ResourceSet<$simpleName>创建失败", e)
    }
  }
}
