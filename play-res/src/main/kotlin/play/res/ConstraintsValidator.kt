package play.res

import jakarta.validation.Validation
import org.hibernate.validator.HibernateValidatorFactory
import play.Log
import play.util.collection.filterDuplicated
import play.util.reflect.Reflect
import java.util.*

internal class ConstraintsValidator(
  private val resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>
) {

  private val validatorFactory = Validation.byDefaultProvider()
    .configure()
    .buildValidatorFactory()
    .unwrap(HibernateValidatorFactory::class.java)

  private val validator = validatorFactory
    .usingContext()
    .constraintValidatorPayload(resourceSets)
    .validator

  fun validateAll(): List<String> = validate(resourceSets)

  fun validateAllThrows() {
    val errors = validate(resourceSets)
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors.joinToString("\n", "\n", ""))
    }
  }

  fun validateThrows(classes: Iterable<Class<*>>) {
    val errors = validate(resourceSets.filterKeys { classes.contains(it) })
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors.joinToString("\n", "\n", ""))
    }
  }

  fun validate(classes: Iterable<Class<*>>): List<String> {
    return validate(resourceSets.filterKeys { classes.contains(it) })
  }

  private fun validate(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>): List<String> {
    Log.info { "开始[配置验证]" }
    val errors = LinkedList<String>()
    resourceSets.asSequence().forEach { e ->
      val clazz = e.key
      val set = e.value
      set.list().asSequence().flatMap { bean ->
        validator.validate(bean).asSequence().map { err ->
          val value = err.constraintDescriptor.attributes["value"]
          val resourceClassName = if (value is Class<*>) value.simpleName else ""
          "$resourceClassName${err.message}: ${err.rootBean.javaClass.simpleName}(${err.rootBean.id}).${err.propertyPath} = ${err.invalidValue}"
        }
      }.forEach { errors.add(it) }

      val fields = Reflect.getAllFields(clazz)
      fields.asSequence()
        .filter { it.isAnnotationPresent(Unique::class.java) }
        .flatMap { f ->
          sequenceOf(set.list().asSequence().map { Reflect.getFieldValue<Any>(f, it) }.filterDuplicated())
            .filter { it.isNotEmpty() }
            .map { "${clazz.simpleName}.${f.name}的值不能重复: $it" }
        }.forEach { errors.add(it) }

      fields.asSequence()
        .filter { it.isAnnotationPresent(Incremental::class.java) }
        .flatMap { f ->
          if (f.type != Byte::class.java || f.type != Short::class.java || f.type != Int::class.java || f.type != Long::class.java) {
            throw UnsupportedOperationException()
          }
          val it = set.list().asSequence().map { Reflect.getFieldValue<Number>(f, it)!!.toLong() }.iterator()
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
          if (set.size().toLong() != next - start - 1) {
            sequenceOf("${clazz.simpleName}.${f.name}的值必须是递增的")
          } else {
            emptySequence()
          }
        }.forEach { errors.add(it) }
    }

    resourceSets.forEach { (clazz, resourceSet) ->
      clazz.getAnnotation(Extend::class.java)?.also { join ->
        val table = resourceSets[join.table.java]
        val illegalIds =
          resourceSet.list().asSequence().filterNot { table?.contains(it.id) ?: false }.map { it.id }.toList()
        val missionIds =
          table?.list()?.asSequence()?.filterNot { resourceSet.contains(it.id) }?.map { it.id }?.toList() ?: emptyList()
        if (illegalIds.isNotEmpty()) {
          errors += "[${clazz.simpleName}]表的数据在[${join.table.simpleName}]表中不存: ${clazz.simpleName}$illegalIds"
        }
        if (missionIds.isNotEmpty()) {
          errors += "[${clazz.simpleName}]表中缺少[${join.table.simpleName}]表的数据: ${join.table.simpleName}$missionIds"
        }
      }
//      clazz.getAnnotation(ExtendGroup::class.java)?.also { join ->
//        val table = configSets[join.table.java]
//        val illegalIds =
//          configSet.sequence().filterNot { table?.containsGroup(it.id) ?: false }.map { it.id }.toList()
//        if (table == null) {
//          errors += "[${clazz.simpleName}]表引用的[${join.table.simpleName}]表不存在"
//        } else {
//          val missing = table.groupMap().keys.filterNot { configSet.contains(it as Int) }.toList()
//          if (illegalIds.isNotEmpty()) {
//            errors += "[${clazz.simpleName}]表的数据在[${join.table.simpleName}]表中不存对应的分组: ${clazz.simpleName}$illegalIds"
//          }
//          if (missing.isNotEmpty()) {
//            errors += "[${clazz.simpleName}]表中缺少[${join.table.simpleName}]表的分组数据: ${join.table.simpleName}$missing"
//          }
//        }
//      }
    }
    Log.info { "完成[配置验证]" }
    return errors
  }
}
