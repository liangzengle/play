package play.util.reflect

import io.github.classgraph.ClassInfoList

object ClassInfoFilters {

  @JvmStatic
  fun ordinaryClass(): ClassInfoList.ClassInfoFilter = ClassInfoList.ClassInfoFilter {
    !it.isAbstract && !it.isAnonymousInnerClass && !it.isEnum && !(it.isInnerClass && !it.isStatic)
  }

  @JvmStatic
  fun hasAnnotation(annotationType: Class<out Annotation>): ClassInfoList.ClassInfoFilter =
    ClassInfoList.ClassInfoFilter {
      it.hasAnnotation(annotationType.name)
    }
}
