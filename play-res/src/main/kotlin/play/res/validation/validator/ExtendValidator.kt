package play.res.validation.validator

import play.res.AbstractResource
import play.res.ResourceSet
import play.res.ResourceSetProvider
import play.res.validation.constraints.Extend

class ExtendValidator : ResourceSetValidator() {
  override fun validate(
    resourceClass: Class<out AbstractResource>,
    resourceSet: ResourceSet<out AbstractResource>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  ) {
    resourceClass.getAnnotation(Extend::class.java)?.also { parent ->
      val parentTable = resourceSetProvider.get(parent.table.java)
      val illegalIds =
        resourceSet.list().asSequence().filterNot { parentTable.contains(it.id) }.map { it.id }.toList()
      val missionIds =
        parentTable.list().asSequence().filterNot { resourceSet.contains(it.id) }.map { it.id }.toList()
      if (illegalIds.isNotEmpty()) {
        errors += "[${resourceClass.simpleName}]表的数据在[${parent.table.simpleName}]表中不存: ${resourceClass.simpleName}$illegalIds"
      }
      if (missionIds.isNotEmpty()) {
        errors += "[${resourceClass.simpleName}]表中缺少[${parent.table.simpleName}]表的数据: ${parent.table.simpleName}$missionIds"
      }
    }
  }
}
