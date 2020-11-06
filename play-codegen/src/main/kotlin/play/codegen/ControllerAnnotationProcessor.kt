package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

@AutoService(Processor::class)
class ControllerAnnotationProcessor : PlayAnnotationProcessor() {

  private lateinit var userClass: ClassName

  companion object {
    private const val ControllerUserClass = "controller.user-class"
  }

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    userClass = processingEnv.options[ControllerUserClass]?.let {
      ClassName.bestGuess(it)
    } ?: LONG
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(Controller.canonicalName)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (annotations.isEmpty()) {
      return false
    }
    val controllerAnnotation = annotations.first()
    val controllers = roundEnv
      .getElementsAnnotatedWith(controllerAnnotation)
      .asSequence()
      .map { it as TypeElement }
      .filterNot { it.isAnnotationPresent(DisableCodegen) }
      .map(::toControllerTypeElement)
      .sortedBy { it.moduleId }
      .toList()
    controllers.forEach(::generate)
    genInvokerManager(controllers)
    return true
  }

  private fun genInvokerManager(controllers: List<ControllerTypeElement>) {
    val ctor = FunSpec.constructorBuilder().addAnnotation(Inject)
    controllers.forEach { c ->
      ctor.addParameter(c.simpleName.toString().decapitalize(), getInvokerClassName(c.typeElement))
    }
    val className = "ControllerInvokerManager"
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton)
      .primaryConstructor(ctor.build())
    controllers.forEach { c ->
      classBuilder.addProperty(
        PropertySpec.builder(
          c.simpleName.toString().decapitalize(),
          getInvokerClassName(c.typeElement),
          KModifier.PRIVATE
        )
          .initializer(c.simpleName.toString().decapitalize())
          .build()
      )
    }
    val self = userClass
    val selfInvoker = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request)
    selfInvoker.beginControlFlow("return when(request.header.msgId.moduleId.toInt()) {")
    controllers.forEach { c ->
      selfInvoker.addStatement("%L -> %L.invoke(self, request)", c.moduleId, c.simpleName.toString().decapitalize())
    }
    selfInvoker.addStatement("else -> null")
    selfInvoker.endControlFlow()
    classBuilder.addFunction(selfInvoker.build())

    if (userClass != LONG) {
      val playerIdInvoker = FunSpec.builder("invoke")
        .addParameter("playerId", Long::class)
        .addParameter("request", Request)
      playerIdInvoker.beginControlFlow("return when(request.header.msgId.moduleId.toInt()) {")
      controllers.filter { c -> c.cmdMethods.any { !parameterHasUser(it) } }.forEach { c ->
        playerIdInvoker.addStatement(
          "%L -> %L.invoke(playerId, request)",
          c.moduleId,
          c.simpleName.toString().decapitalize()
        )
      }
      playerIdInvoker.addStatement("else -> null")
      playerIdInvoker.endControlFlow()
      classBuilder.addFunction(playerIdInvoker.build())
    }


    val format = FunSpec.builder("formatToString")
      .addParameter("request", Request)
    val formatCode = CodeBlock.builder()
    formatCode.beginControlFlow("return when(request.header.msgId.moduleId.toInt()) {")
    controllers.forEach { c ->
      formatCode.addStatement("%L -> %L.formatToString(request)", c.moduleId, c.simpleName.toString().decapitalize())
    }
    formatCode.addStatement("else -> request.toString()")
    formatCode.endControlFlow()
    format.addCode(formatCode.build())

    classBuilder.addFunction(format.build())

    val file = File(generatedSourcesRoot)
    FileSpec.builder("", className)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)
  }

  private fun getInvokerClassName(typeElement: TypeElement): ClassName {
    return ClassName.bestGuess(typeElement.qualifiedName.toString() + "Invoker")
  }

  private fun generate(typeElement: ControllerTypeElement) {
    val pkg = typeElement.typeElement.getPackage()
    val className = getInvokerClassName(typeElement.typeElement)
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject)
          .addParameter("controller", typeElement.typeElement.asClassName())
          .build()
      )
      .addProperty(
        PropertySpec.builder("controller", typeElement.typeElement.asClassName(), KModifier.PRIVATE)
          .initializer("controller")
          .build()
      )
    val moduleIdGetter = FunSpec.builder("get()")
      .addStatement("return controller.moduleId")
      .build()
    val moduleId = PropertySpec.builder("moduleId", Short::class).getter(moduleIdGetter).build()

    val initCode = CodeBlock.builder()
    initCode.beginControlFlow("if (controller.moduleId != MODULE_ID)")
    initCode.addStatement("throw IllegalStateException(\"%L的模块id不一致\")", typeElement.simpleName)
    initCode.endControlFlow()
    classBuilder.addInitializerBlock(initCode.build())

    classBuilder.addProperty(moduleId)
    classBuilder.addFunction(genSelfInvoke(typeElement))
    if (userClass != LONG) {
      classBuilder.addFunction(genPlayerIdInvoke(typeElement))
    }
    classBuilder.addFunction(genFormat(typeElement))
    classBuilder.addType(genCompanionObject(typeElement))

    val file = File(generatedSourcesRoot)
    FileSpec.builder(pkg, className.simpleName)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)
  }

  private fun genCompanionObject(typeElement: ControllerTypeElement): TypeSpec {
    val builder = TypeSpec.companionObjectBuilder()
    builder.addProperty(
      PropertySpec.builder("MODULE_ID", Short::class, KModifier.CONST)
        .initializer("%L", typeElement.moduleId)
        .build()
    )

    typeElement
      .cmdElements()
      .forEach { elem ->
        val cmd = elem.cmd
        val name = elem.simpleName.toString()
        builder.addProperty(
          PropertySpec.builder(name, Int::class, KModifier.CONST)
            .initializer("%L.toInt() shl 8 or %L", "MODULE_ID", cmd)
            .build()
        )
        if (elem is VariableElement) {
          builder.addProperty(
            PropertySpec.builder(name + "Push", elem.asType().javaToKotlinType())
              .initializer("%T(%L, %L)", elem.asType().javaToKotlinType(), "MODULE_ID", cmd)
              .build()
          )
        }
      }
    return builder.build()
  }

  private fun genFormat(typeElement: ControllerTypeElement): FunSpec {
    val format = FunSpec.builder("formatToString")
      .addParameter("request", Request)
      .returns(String::class)

    val self = userClass
    val code = CodeBlock.builder()
    code.addStatement("request.body.reset()")
    code.beginControlFlow("return when(request.header.msgId.cmd.toInt())")
    for (method in typeElement.cmdMethods) {
      val cmd = method.cmd
      code.addStatement("%L -> ", cmd)
      code.indent()
      val tpl = StringBuilder()
      tpl.append('$').append("moduleId").append(',').append(' ').append(cmd).append(':').append(' ')
      tpl.append(typeElement.simpleName).append('.').append(method.simpleName).append('(')
      for (i in method.parameters.indices) {
        val p = method.parameters[i]
        if (i != 0) {
          tpl.append(',').append(' ')
        }
        val pTypeName = p.asType().asTypeName()
        if (pTypeName != self && pTypeName != Request) {

          tpl.append('$').append('{')
          tpl.append(readStmt(p))
          if (p.asType().kind == TypeKind.ARRAY) {
            tpl.append(".contentToString()")
          }
          tpl.append('}')
        } else {
          tpl.append(p.simpleName)
        }
      }
      tpl.append(')')
      code.add("%P", tpl.toString())
      code.unindent()
      code.add("\n")
    }
    code.addStatement("else -> request.toString()")
    code.endControlFlow()
    return format.addCode(code.build()).build()
  }

  private fun genSelfInvoke(typeElement: ControllerTypeElement): FunSpec {
    val self = userClass
    val func = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request)
      .returns(RequestResult.parameterizedBy(STAR).copy(true))

    val code = CodeBlock.builder()
    code.beginControlFlow("return when(request.header.msgId.cmd.toInt())")

    for (method in typeElement.cmdMethods) {
      if (!parameterHasUser(method)) {
        continue
      }
      val dummy = getAnnotationValue(method, Cmd, "dummy", false)
      if (dummy) {
        continue
      }
      val cmd = getAnnotationValue(method, Cmd, "value", 0.toByte())
      code.beginControlFlow("%L -> ", cmd)
      for (parameter in method.parameters) {
        if (isUserClass(parameter) || isRequest(parameter)) {
          continue
        }
        code.addStatement("val %L =  %L", parameter.simpleName, readStmt(parameter))
      }
      code.addStatement(
        "controller.%L(%L)",
        method.simpleName,
        method.parameters.joinToString(", ") { it.simpleName }
      )
      code.endControlFlow()
    }
    code.addStatement("else -> null")
    code.endControlFlow()
    return func.addCode(code.build()).build()
  }

  private fun genPlayerIdInvoke(typeElement: ControllerTypeElement): FunSpec {
    val func = FunSpec.builder("invoke")
      .addParameter("playerId", Long::class)
      .addParameter("request", Request)
      .returns(RequestResult.parameterizedBy(STAR).copy(true))

    val code = CodeBlock.builder()
    code.beginControlFlow("return when(request.header.msgId.cmd.toInt())")

    for (method in typeElement.cmdMethods) {
      if (parameterHasUser(method)) {
        continue
      }
      val dummy = getAnnotationValue(method, Cmd, "dummy", false)
      if (dummy) {
        continue
      }
      val cmd = getAnnotationValue(method, Cmd, "value", 0.toByte())
      code.beginControlFlow("%L -> ", cmd)
      for (parameter in method.parameters) {
        if (isPlayerId(parameter) || isRequest(parameter)) {
          continue
        }
        code.addStatement("val %L =  %L", parameter.simpleName, readStmt(parameter))
      }
      code.addStatement(
        "controller.%L(%L)",
        method.simpleName,
        method.parameters.joinToString(", ") { it.simpleName }
      )
      code.endControlFlow()
    }
    code.addStatement("else -> null")
    code.endControlFlow()
    return func.addCode(code.build()).build()
  }

  private fun isPlayerId(parameter: VariableElement): Boolean {
    return parameter.simpleName.contentEquals("playerId")
      && parameter.asType().asTypeName() == Long::class.asTypeName()
  }

  private fun isRequest(parameter: VariableElement): Boolean {
    return parameter.asType().asTypeName() == Request
  }

  private fun isUserClass(parameter: VariableElement): Boolean {
    return parameter.asType().asTypeName() == userClass
  }

  private fun parameterHasUser(elem: CmdExecutableElement): Boolean {
    return elem.element.parameters.any { it.asType().toString() == userClass.canonicalName }
  }

  private fun readStmt(element: VariableElement): String {
    val type = element.asType()
    val reader = when (type.kind) {
      TypeKind.BOOLEAN -> "readBoolean()"
      TypeKind.BYTE -> "readByte()"
      TypeKind.INT -> "readInt()"
      TypeKind.LONG -> "readLong()"
      TypeKind.DOUBLE -> "readDouble()"
      TypeKind.ARRAY -> {
        val componentType = (element.asType() as ArrayType).componentType
        when (componentType.kind) {
          TypeKind.BYTE -> "readByteArray()"
          TypeKind.INT -> "readIntArray()"
          TypeKind.LONG -> "readLongArray()"
          TypeKind.DOUBLE -> "readDoubleArray()"
          TypeKind.DECLARED -> {
            componentType as DeclaredType
            if (componentType.asElement().simpleName.contentEquals("String")) {
              "readStringArray()"
            } else {
              throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
            }
          }
          else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
        }
      }
      TypeKind.DECLARED -> {
        val declaredType = element.asType() as DeclaredType
        if (typeUtils.isSameType(declaredType, elementUtils.getTypeElement(String::class.java.name).asType())) {
          "readString()"
        } else if (isList(declaredType)) {
          val typeMirror = declaredType.typeArguments[0]
          when (typeMirror.kind) {
            TypeKind.BYTE -> "readByteList()"
            TypeKind.INT -> "readIntList()"
            TypeKind.LONG -> "readLongList()"
            TypeKind.DECLARED -> "readStringList()"
            else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
          }
        } else {
          throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
        }
      }
      else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
    }
    return "request.body.$reader"
  }

  private fun toControllerTypeElement(typeElement: TypeElement): ControllerTypeElement {
    val moduleId = getAnnotationValue(typeElement, Controller, "moduleId", 0.toShort())
    val cmdMethods = typeElement.enclosedElements
      .asSequence()
      .filter { it.kind == ElementKind.METHOD && it.isAnnotationPresent(Cmd) }
      .map { element ->
        val cmd = getAnnotationValue(element, Cmd, "value", 0.toByte())
        val dummy = getAnnotationValue(element, Cmd, "dummy", false)
        CmdExecutableElement(cmd, dummy, element as ExecutableElement)
      }
      .toList()

    val cmdVariables = typeElement.enclosedElements
      .asSequence()
      .filter { it.kind == ElementKind.FIELD && it.isAnnotationPresent(Cmd) }
      .map { element ->
        val cmd = getAnnotationValue(element, Cmd, "value", 0.toByte())
        val dummy = getAnnotationValue(element, Cmd, "dummy", false)
        CmdVariableElement(cmd, dummy, element as VariableElement)
      }
      .toList()
    return ControllerTypeElement(moduleId, typeElement, cmdMethods, cmdVariables)
  }

  private data class ControllerTypeElement(
    val moduleId: Short,
    val typeElement: TypeElement,
    val cmdMethods: List<CmdExecutableElement>,
    val cmdVariables: List<CmdVariableElement>
  ) {
    val simpleName: Name get() = typeElement.simpleName
    fun cmdElements(): Sequence<CmdElement> = cmdMethods.asSequence() + cmdVariables.asSequence()
  }

  private interface CmdElement : Element {
    val cmd: Byte
    val dummy: Boolean
  }

  private data class CmdExecutableElement(
    override val cmd: Byte,
    override val dummy: Boolean,
    val element: ExecutableElement
  ) : ExecutableElement by element, CmdElement

  private data class CmdVariableElement(
    override val cmd: Byte,
    override val dummy: Boolean,
    val element: VariableElement
  ) : VariableElement by element, CmdElement
}
