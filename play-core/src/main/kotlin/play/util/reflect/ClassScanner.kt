package play.util.reflect

interface ClassScanner {
  fun <T> getInstantiatableSubclasses(superType: Class<T>): Collection<Class<T>>

  fun getInstantiatableClassesAnnotatedWith(annotationType: Class<out Annotation>): Collection<Class<*>>
}
