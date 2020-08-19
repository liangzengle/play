package play.config

import org.hibernate.validator.HibernateValidatorFactory
import org.reflections.ReflectionUtils
import play.Log
import play.util.collection.filterDuplicated
import play.util.reflect.getUnchecked
import java.util.*
import javax.validation.Validation

internal class ConstraintsValidator(
  private val configSets: Map<Class<AbstractConfig>, AnyConfigSet>
) {

  private val validatorFactory = Validation.byDefaultProvider()
    .configure()
    .buildValidatorFactory()
    .unwrap(HibernateValidatorFactory::class.java)

  private val validator = validatorFactory
    .usingContext()
    .constraintValidatorPayload(configSets)
    .validator

  fun validateAll() = validate(configSets)

  fun validate(classes: Iterable<Class<*>>) {
    validate(configSets.filterKeys { classes.contains(it) })
  }

  private fun validate(configSets: Map<Class<AbstractConfig>, AnyConfigSet>) {
    Log.info { "开始[验证配置]" }
    val errors = LinkedList<String>()
    configSets.asSequence().forEach { e ->
      val clazz = e.key
      val set = e.value
      set.sequence().flatMap { bean ->
        validator.validate(bean).asSequence().map { err ->
          val value = err.constraintDescriptor.attributes["value"]
          val configClassName = if (value is Class<*>) value.simpleName else ""
          "$configClassName${err.message}: ${err.rootBean.javaClass.simpleName}(${err.rootBean.id}).${err.propertyPath} = ${err.invalidValue}"
        }
      }.forEach { errors.add(it) }

      val fields = ReflectionUtils.getAllFields(clazz)
      fields.asSequence()
        .filter { it.isAnnotationPresent(Unique::class.java) }
        .flatMap { f ->
          sequenceOf(set.sequence().map { f.getUnchecked<Any>(it) }.filterDuplicated())
            .filter { it.isNotEmpty() }
            .map { "${clazz.simpleName}.${f.name}的值不能重复: $it" }
        }.forEach { errors.add(it) }


      fields.asSequence()
        .filter { it.isAnnotationPresent(Incremental::class.java) }
        .flatMap { f ->
          if (f.type != Byte::class.java || f.type != Short::class.java || f.type != Int::class.java || f.type != Long::class.java) {
            throw UnsupportedOperationException()
          }
          val it = set.sequence().map { f.getUnchecked<Number>(it)!!.toLong() }.iterator()
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

    configSets.forEach { (clazz, configSet) ->
      clazz.getAnnotation(Join::class.java)?.also { join ->
        val table = configSets[join.table.java]
        val illegalIds = configSet.sequence().filterNot { table?.contains(it.id) ?: false }.map { it.id }.toList()
        val missionIds =
          table?.sequence()?.filterNot { configSet.contains(it.id) }?.map { it.id }?.toList() ?: emptyList()
        if (illegalIds.isNotEmpty()) {
          errors += "[${clazz.simpleName}]表的数据在[${join.table.simpleName}]表中不存: ${clazz.simpleName}$illegalIds"
        }
        if (missionIds.isNotEmpty()) {
          errors += "[${clazz.simpleName}]表中缺少[${join.table.simpleName}]表的数据: ${join.table.simpleName}$missionIds"
        }
      }
      clazz.getAnnotation(JoinGroup::class.java)?.also { join ->
        val table = configSets[join.table.java]
        val illegalIds =
          configSet.sequence().filterNot { table?.containsGroup(it.id) ?: false }.map { it.id }.toList()
        if (table == null) {
          errors += "[${clazz.simpleName}]表引用的[${join.table.simpleName}]表不存在"
        } else {
          val missing = table.groupMap().keys.filterNot { configSet.contains(it as Int) }.toList()
          if (illegalIds.isNotEmpty()) {
            errors += "[${clazz.simpleName}]表的数据在[${join.table.simpleName}]表中不存对应的分组: ${clazz.simpleName}$illegalIds"
          }
          if (missing.isNotEmpty()) {
            errors += "[${clazz.simpleName}]表中缺少[${join.table.simpleName}]表的分组数据: ${join.table.simpleName}$missing"
          }
        }
      }
    }

    if (errors.isNotEmpty()) {
      throw InvalidConfigException(errors.joinToString("\n", "\n", ""))
    }
    Log.info { "完成[验证配置]" }
  }
}
