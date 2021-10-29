package play.res.validation.validator

import play.res.AbstractResource
import play.res.ResourceSet
import play.res.ResourceSetProvider
import play.res.validation.constraints.Incremental
import play.util.reflect.Reflect

class IncrementalFieldValidator : ResourceSetValidator() {
  override fun validate(
    resourceClass: Class<out AbstractResource>,
    resourceSet: ResourceSet<out AbstractResource>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  ) {
    val fields = Reflect.getAllFields(resourceClass)
    fields.asSequence()
      .filter { it.isAnnotationPresent(Incremental::class.java) }
      .flatMap { f ->
        if (f.type != Byte::class.java || f.type != Short::class.java || f.type != Int::class.java || f.type != Long::class.java) {
          throw UnsupportedOperationException()
        }
        val it = resourceSet.list().asSequence().map { Reflect.getFieldValue<Number>(f, it)!!.toLong() }.iterator()
        val start = if (it.hasNext()) it.next() else 0L
        var next = start + 1
        while (it.hasNext()) {
          val value = it.next()
          if (value != next) {
            break
          } else {
            next += 1
          }
        }
        if (resourceSet.size().toLong() != next - start - 1) {
          sequenceOf("${resourceClass.simpleName}.${f.name}的值必须是递增的")
        } else {
          emptySequence()
        }
      }.forEach { errors.add(it) }
  }
}
