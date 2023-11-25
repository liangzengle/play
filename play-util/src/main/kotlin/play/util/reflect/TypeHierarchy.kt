package play.util.reflect

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.common.reflect.TypeToken
import play.util.LambdaClassValue

/**
 * 类继承结构的缓存
 *
 * @author LiangZengle
 */
object TypeHierarchy {

  private val cache = LambdaClassValue { type -> ImmutableSet.copyOf(TypeToken.of(type).types.rawTypes()) }

  /**
   * 获取[type]的所有父类及接口
   *
   * @param type 当前类
   * @param includeSelf 返回的结果是否包含自身
   * @param includeObject 返回的结果是否包含Object.class
   * @return 所有父类及接口的class
   */
  @JvmStatic
  fun get(type: Class<*>, includeSelf: Boolean, includeObject: Boolean): Set<Class<*>> {
    if (includeSelf && includeObject) {
      return cache.get(type)
    }
    if (includeSelf) {
      return Sets.filter(cache.get(type)) { it !== Any::class.java }
    }
    if (includeObject) {
      return Sets.filter(cache.get(type)) { it !== type }
    }
    return Sets.filter(cache.get(type)) { it !== Any::class.java && it !== type }
  }
}
