package play.codegen.ksp.controller.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.Request
import play.codegen.RequestCommander
import play.codegen.RequestResult
import play.codegen.Result2
import play.codegen.ksp.controller.isRequestCommander
import play.codegen.ksp.controller.readStmt
import play.codegen.ksp.toParamString
import play.codegen.ksp.toTypeName2

class ControllerInvokerTypeSpec(
  private val controllerClass: ControllerClassDeclaration,
  private val injectAnnotation: AnnotationSpec,
  private val singletonAnnotations: List<AnnotationSpec>
) {

  fun build(): TypeSpec {
    val controllerClassName = controllerClass.toClassName()
    val invokerClass =
      ClassName.bestGuess(controllerClass.packageName.asString() + '.' + controllerClass.invokerClassName)
    val classBuilder =
      TypeSpec.classBuilder(invokerClass).addAnnotations(singletonAnnotations).primaryConstructor(
        FunSpec.constructorBuilder().addAnnotation(injectAnnotation)
          .addParameter("controller", controllerClassName).build()
      ).addProperty(
        PropertySpec.builder("controller", controllerClassName).initializer("controller").build()
      )
    val moduleId = PropertySpec.builder("moduleId", Short::class).getter(
      FunSpec.builder("get()").addStatement("return controller.moduleId").build()
    ).build()
    val init = CodeBlock.builder().beginControlFlow("if (controller.moduleId != MODULE_ID)")
      .addStatement("throw IllegalStateException(%P)", "Inconsistent moduleId: \${controller.moduleId} vs \$MODULE_ID")
      .endControlFlow()
      .build()
    classBuilder.addInitializerBlock(init)
    classBuilder.addProperty(moduleId)
    classBuilder.addFunction(buildInvokeFunSpec())
    classBuilder.addFunction(buildRequestFormatFunSpec())
    classBuilder.addType(buildCompanionObjectTypeSpec())

    return classBuilder.build()
  }

  private fun buildInvokeFunSpec(): FunSpec {
    val code = CodeBlock.builder()
    code.beginControlFlow("return when(request.header.msgId.cmd.toInt())")

    for (func in controllerClass.commandFunctions) {
      if (func.dummy) {
        continue
      }
      val cmd = func.cmd
      code.beginControlFlow("%L -> ", cmd)
      for (parameter in func.parameters) {
        val parameterType = parameter.type.toTypeName2()
        if (parameterType == Request) {
          continue
        }
        val parameterName = parameter.name!!.asString()
        if (isRequestCommander(parameter)) {
          code.addStatement("val %L = _commander as %T", parameterName, parameterType)
          continue
        }
        code.addStatement("val %L = %L", parameterName, readStmt(parameter))
      }
      val returnsResult = func.returnType?.resolve()!!.toClassName() == Result2
      if (returnsResult) {
        code.add("val result = ")
      }
      code.addStatement(
        "controller.%L(%L)", func.simpleName.asString(), toParamString(func.parameters)
      )
      if (returnsResult) {
        code.addStatement("%T(result)", RequestResult)
      }
      code.endControlFlow()
    }
    code.addStatement("else -> null")
    code.endControlFlow()
    return FunSpec.builder("invoke").addParameter("_commander", RequestCommander).addParameter("request", Request)
      .returns(RequestResult.parameterizedBy(STAR).copy(true)).addCode(code.build()).build()
  }

  private fun buildRequestFormatFunSpec(): FunSpec {
    val format = FunSpec.builder("formatToString").addParameter("request", Request).returns(String::class)

    val code = CodeBlock.builder()
    code.addStatement("request.body.reset()")
    code.beginControlFlow("return when(request.header.msgId.cmd.toInt())")
    for (func in controllerClass.commandFunctions) {
      val cmd = func.cmd
      code.addStatement("%L -> ", cmd)
      code.indent()
      val tpl = StringBuilder()
      tpl.append('$').append("moduleId").append(',').append(' ').append(cmd).append(':').append(' ')
      tpl.append(controllerClass.simpleName.asString()).append('.').append(func.simpleName.asString())
        .append('(')
      for (i in func.parameters.indices) {
        val p = func.parameters[i]
        if (i != 0) {
          tpl.append(',').append(' ')
        }
        val paramClassName = p.type.resolve().toClassName()
        if (paramClassName != Request && !isRequestCommander(p)) {
          tpl.append('$').append('{')
          tpl.append(readStmt(p))
          // TODO array type detect
          if (paramClassName.canonicalName.endsWith("[]")) {
            tpl.append(".contentToString()")
          }
          tpl.append('}')
        } else {
          tpl.append(p.name?.asString())
        }
      }
      tpl.append(')')
      code.addStatement("%P", tpl.toString())
      code.unindent()
    }
    code.addStatement("else -> request.toString()")
    code.endControlFlow()
    return format.addCode(code.build()).build()
  }

  private fun buildCompanionObjectTypeSpec(): TypeSpec {
    val builder = TypeSpec.companionObjectBuilder()
    builder.addProperty(
      PropertySpec.builder("MODULE_ID", Short::class, KModifier.CONST).initializer("%L", controllerClass.moduleId)
        .build()
    )

    for (func in controllerClass.commandFunctions) {
      val cmd = func.cmd
      val name = func.simpleName.asString()
      builder.addProperty(
        PropertySpec.builder(name, Int::class, KModifier.CONST).initializer("%L.toInt() shl 8 or %L", "MODULE_ID", cmd)
          .build()
      )
    }

    for (property in controllerClass.commandProperties) {
      val cmd = property.cmd
      val name = property.simpleName.asString()
      builder.addProperty(
        PropertySpec.builder(name, Int::class, KModifier.CONST).initializer("%L.toInt() shl 8 or %L", "MODULE_ID", cmd)
          .build()
      )
      val type = property.type.toTypeName2()
      builder.addProperty(
        PropertySpec.builder(name + "Push", type).initializer("%T(%L, %L)", type, "MODULE_ID", cmd).build()
      )
    }
    return builder.build()
  }
}
