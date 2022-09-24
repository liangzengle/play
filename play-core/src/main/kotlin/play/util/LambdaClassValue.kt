package play.util

/**
 * @author LiangZengle
 */
class LambdaClassValue<T>(private val mapper: (Class<*>) -> T) : ClassValue<T>() {
  override fun computeValue(type: Class<*>): T {
    return mapper(type)
  }
}
