package play.codegen.ksp.controller.model

import com.squareup.kotlinpoet.*
import play.codegen.Request
import play.codegen.RequestCommander

class RequestDispatcherTypeSpec(
  private val controllers: Collection<ControllerClassDeclaration>,
  private val injectAnnotation: AnnotationSpec?,
  private val singletonAnnotations: List<AnnotationSpec>
) {

  fun build(): TypeSpec {
    val orderedControllers = controllers.sortedBy { it.moduleId }
    val ctor = FunSpec.constructorBuilder().apply { injectAnnotation?.also(::addAnnotation) }
    for (c in orderedControllers) {
      ctor.addParameter(c.invokerVarName, c.invokerClassName)
    }
    val className = "RequestDispatcher"
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotations(singletonAnnotations)
      .primaryConstructor(ctor.build())
    for (c in orderedControllers) {
      classBuilder.addProperty(
        PropertySpec
          .builder(c.invokerVarName, c.invokerClassName, KModifier.PRIVATE)
          .initializer(c.invokerVarName)
          .build()
      )
    }
    val dispatcherFuncBuilder = FunSpec.builder("invoke")
      .addParameter("commander", RequestCommander)
      .addParameter("request", Request)
    dispatcherFuncBuilder.beginControlFlow("return when(request.header.msgId.moduleId.toInt())")
    for (c in controllers) {
      dispatcherFuncBuilder.addStatement("%L -> %L.invoke(commander, request)", c.moduleId, c.invokerVarName)
    }
    dispatcherFuncBuilder.addStatement("else -> null")
    dispatcherFuncBuilder.endControlFlow()
    classBuilder.addFunction(dispatcherFuncBuilder.build())

    val format = FunSpec.builder("formatToString")
      .addParameter("request", Request)
    val formatCode = CodeBlock.builder()
    formatCode.beginControlFlow("return when(request.header.msgId.moduleId.toInt())")
    controllers.forEach { c ->
      formatCode.addStatement("%L -> %L.formatToString(request)", c.moduleId, c.invokerVarName)
    }
    formatCode.addStatement("else -> request.toString()")
    formatCode.endControlFlow()
    format.addCode(formatCode.build())

    classBuilder.addFunction(format.build())

    return classBuilder.build()
  }
}
