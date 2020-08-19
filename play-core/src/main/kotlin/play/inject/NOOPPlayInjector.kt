package play.inject

/**
 *
 * @author LiangZengle
 */
object NOOPPlayInjector : PlayInjector {
  override fun <T> getInstance(type: Class<T>): T {
    throw UnsupportedOperationException()
  }

  override fun <T> getInstance(type: Class<T>, name: String): T {
    throw UnsupportedOperationException()
  }

  override fun <T> getInstancesOfType(type: Class<T>): Collection<T> {
    return emptyList()
  }

  override fun getInstancesAnnotatedWith(type: Class<out Annotation>): Collection<Any> {
    return emptyList()
  }
}
