package play.config.validation

import play.config.AbstractConfig
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@Constraint(validatedBy = [ReferToConstraintValidator::class])
annotation class ReferTo(
  val value: KClass<out AbstractConfig>,
  val message: String = "{javax.validation.constraints.ReferTo.message}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@Constraint(validatedBy = [ReferToGroupConstraintValidator::class])
annotation class ReferToGroup(
  val value: KClass<out AbstractConfig>, val message: String = "{javax.validation.constraints.ReferToGroup.message}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)
