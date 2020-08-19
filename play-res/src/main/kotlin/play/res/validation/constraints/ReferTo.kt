package play.res.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import play.res.AbstractResource
import play.res.validation.validator.ReferToConstraintValidator
import kotlin.reflect.KClass

/**
 * 表示该字段引用了另一个表的id
 *
 * @property value 引用的表
 * @property message 错误信息
 * @property groups
 * @property payload
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@Constraint(validatedBy = [ReferToConstraintValidator::class])
annotation class ReferTo(
  val value: KClass<out AbstractResource>,
  val message: String = "{javax.validation.constraints.ReferTo.message}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)
