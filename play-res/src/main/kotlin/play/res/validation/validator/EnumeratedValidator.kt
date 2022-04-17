package play.res.validation.validator

import play.res.ResourceSetProvider
import play.res.validation.constraints.Enumerated
import play.util.enumeration.Enums
import play.util.enumeration.IdEnum

class EnumeratedValidator : ResourceValidator() {
  override fun validate(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    for ((clazz, resourceSet) in resourceSetProvider.resourceSetMap) {
      val enumerated = clazz.getAnnotation(Enumerated::class.java) ?: continue
      val enumClass = enumerated.value.java
      val idSet = Enums.iterator(enumClass).asSequence().filterIsInstance<IdEnum>().map { it.id() }.toSet()
      for (resource in resourceSet.list()) {
        val id = resource.id
        if (!idSet.contains(id)) {
          errors.add("枚举不存在: ${clazz.simpleName}.id = ${resource.id}")
        }
      }
      for (id in idSet) {
        if (!resourceSet.contains(id)) {
          errors.add("${clazz.simpleName}缺少配置:$id")
        }
      }
    }
  }
}
