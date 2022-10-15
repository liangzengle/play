package play.dokka.model

/**
 *
 * @author LiangZengle
 */
data class ClassDescriptorList(val classes: Map<String, ClassDescriptor>) {

  fun query(clazz: Class<*>): ClassDescriptor? {
    return classes[clazz.name]
  }
}
