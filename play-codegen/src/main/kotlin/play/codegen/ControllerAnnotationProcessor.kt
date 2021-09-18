package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.util.*
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
  private lateinit var managerPackage: String

  private val controllerTypeElements = LinkedList<ControllerTypeElement>()

  companion object {
    private const val ControllerUserClass = "controller.user-class"
    private const val ControllerManagerPackage = "controller.manager.package"
  }

  override fun init0(processingEnv: ProcessingEnvironment) {
    userClass = processingEnv.options[ControllerUserClass]?.let {
      ClassName.bestGuess(it)
    } ?: LONG
    managerPackage = processingEnv.options[ControllerManagerPackage] ?: ""
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(Controller.canonicalName)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.processingOver()) {
      genInvokerManager()
    } else {
      roundEnv
        .getElementsAnnotatedWith(Controller.asTypeElement())
        .asSequence()
        .map { it as TypeElement }
        .filterNot { it.isAnnotationPresent(DisableCodegen) }
        .map(::toControllerTypeElement)
        .forEach {
          generateInvoker(it)
          controllerTypeElements.add(it)
        }
    }
    return true
  }

  private fun genInvokerManager() {
    val controllers = controllerTypeElements.toTypedArray()
    controllers.sortBy { it.moduleId }
    val ctor = FunSpec.constructorBuilder().addAnnotation(iocInjectAnnotation())
    for (c in controllers) {
      ctor.addParameter(c.invokerVarName, c.invokerClassName)
    }
    val className = "ControllerInvokerManager"
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotations(iocSingletonAnnotations())
      .primaryConstructor(ctor.build())
    for (c in controllers) {
      classBuilder.addProperty(
        PropertySpec
          .builder(c.invokerVarName, c.invokerClassName, KModifier.PRIVATE)
          .initializer(c.invokerVarName)
          .build()
      )
    }
    val self = userClass
    val selfInvoker = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request)
    selfInvoker.beginControlFlow("return when(request.header.msgId.moduleId.toInt())")
    for (c in controllers) {
      selfInvoker.addStatement("%L -> %L.invoke(self, request)", c.moduleId, c.invokerVarName)
    }
    selfInvoker.addStatement("else -> null")
    selfInvoker.endControlFlow()
    classBuilder.addFunction(selfInvoker.build())

    if (userClass != LONG) {
      val playerIdInvoker = FunSpec.builder("invoke")
        .addParameter("playerId", Long::class)
        .addParameter("request", Request)
      playerIdInvoker.beginControlFlow("return when(request.header.msgId.moduleId.toInt())")
      for (c in controllers) {
        if (c.cmdMethods.any { !parameterHasUser(it) }) {
          playerIdInvoker.addStatement(
            "%L -> %L.invoke(playerId, request)",
            c.moduleId,
            c.invokerVarName
          )
        }
      }
      playerIdInvoker.addStatement("else -> null")
      playerIdInvoker.endControlFlow()
      classBuilder.addFunction(playerIdInvoker.build())
    }

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

    val file = File(generatedSourcesRoot)
    FileSpec.builder(managerPackage, className)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)
  }

  private fun getInvokerClassName(typeElement: TypeElement): ClassName {
    val controllerName = typeElement.qualifiedName.toString()
    return ClassName.bestGuess(controllerName.removeSuffix("Controller") + "Module")
  }

  private fun generateInvoker(typeElement: ControllerTypeElement) {
    val pkg = typeElement.typeElement.getPackage()
    val controllerClassName = typeElement.typeElement.asClassName()
    val invokerClassName = typeElement.invokerClassName
    val classBuilder = TypeSpec.classBuilder(invokerClassName)
      .addAnnotations(iocSingletonAnnotations())
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(iocInjectAnnotation())
          .addParameter("controller", controllerClassName)
          .build()
      )
      .addProperty(
        PropertySpec.builder("controller", controllerClassName, KModifier.PRIVATE)
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
    FileSpec.builder(pkg, invokerClassName.simpleName)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)

    generateRequestMessages(typeElement)
  }

  private fun generateRequestMessages(typeElement: ControllerTypeElement) {
    if (!typeElement.typeElement.isAnnotationPresent(GeneratePlayerRequestMessage)) {
      return
    }
    val interfaceType =
      getAnnotationValue<DeclaredType>(typeElement.typeElement, GeneratePlayerRequestMessage, "interfaceType")!!
    val moduleName = getModuleName(typeElement)
    val converterSimpleName = moduleName + "MessageConverter"
    val objectBuilder = TypeSpec.objectBuilder(converterSimpleName)
    objectBuilder.superclass(MessageConverter)
    val convert = FunSpec.builder("convert")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("pr", PlayerRequest)
      .returns(interfaceType.toClassName())
    val codeBlock = CodeBlock.builder()
    codeBlock.beginControlFlow("return when(pr.cmd.toInt())")

    val baseModuleMessage =
      TypeSpec.classBuilder(moduleName + "PlayerRequest").addModifiers(KModifier.PUBLIC, KModifier.OPEN)
        .superclass(AbstractPlayerRequest)
        .addSuperinterface(interfaceType.toClassName())
        .addSuperclassConstructorParameter("playerId, request")
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("playerId", LONG)
            .addParameter("request", Request)
            .build()
        ).build()

    val messagePackage = typeElement.typeElement.getPackage() + ".message"

    val messageTypes = arrayListOf<TypeSpec>()
    for (method in typeElement.cmdMethods) {
      val messageSimpleName = method.simpleName.toString().capitalize() + moduleName + "Request"

      val messageClassName = ClassName.bestGuess("$messagePackage.$messageSimpleName")
      val messageBuilder = TypeSpec.classBuilder(messageSimpleName)
        .superclass(AbstractPlayerRequest)
        .addSuperclassConstructorParameter("playerId, request")
        .addSuperinterface(interfaceType.asTypeName())
      val messageCtorBuilder = FunSpec.constructorBuilder()
        .addParameter("playerId", LONG)
        .addParameter("request", Request)

      val methodName = "to$messageSimpleName"
      val b = FunSpec.builder(methodName).addParameter("pr", PlayerRequest).returns(messageClassName)
      b.addStatement("val playerId = pr.playerId")
      b.addStatement("val request = pr.request")
      val paramList = StringBuilder()
      paramList.append("playerId, request")
      for (parameter in method.parameters) {
        if (!isPlayerId(parameter)) {
          b.addStatement("val %L = %L", parameter.simpleName, readStmt(parameter))
          messageCtorBuilder.addParameter(parameter.simpleName.toString(), parameter.asType().javaToKotlinType())
          messageBuilder.addProperty(
            PropertySpec.builder(
              parameter.simpleName.toString(),
              parameter.asType().javaToKotlinType(),
              KModifier.PUBLIC
            )
              .addAnnotation(JvmField::class)
              .initializer(parameter.simpleName.toString())
              .build()
          )
          paramList.append(',').append(' ').append(parameter.simpleName)
        }
      }
      messageBuilder.primaryConstructor(messageCtorBuilder.build())
      messageTypes.add(messageBuilder.build())
      b.addStatement("return %T(%L)", messageClassName, paramList.toString())
      objectBuilder.addFunction(b.build())
      codeBlock.addStatement("%L -> %L(pr)", method.cmd, methodName)
    }
    codeBlock.addStatement("else -> %L(pr.playerId, pr.request)", baseModuleMessage.name)
    codeBlock.endControlFlow()
    convert.addCode(codeBlock.build())
    objectBuilder.addFunction(convert.build())

    val file = File(generatedSourcesRoot)
    val fileBuilder = FileSpec.builder(messagePackage, "Request")
    fileBuilder.addType(objectBuilder.build())
    fileBuilder.addType(baseModuleMessage)
    for (messageType in messageTypes) {
      fileBuilder.addType(messageType)
    }
    fileBuilder.build().writeTo(file)
  }

  private fun getModuleName(typeElement: ControllerTypeElement): String {
    val controllerSimpleName = typeElement.simpleName.toString()
    return if (controllerSimpleName.endsWith("Controller")) {
      controllerSimpleName.dropLast("Controller".length)
    } else {
      controllerSimpleName
    }
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
      code.addStatement("%P", tpl.toString())
      code.unindent()
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
    return parameter.simpleName.contentEquals("playerId") &&
      parameter.asType().javaToKotlinType() == Long::class.asTypeName()
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
          TypeKind.BYTE -> "getByteArray()"
          TypeKind.INT -> "getIntArray()"
          TypeKind.LONG -> "getLongArray()"
          TypeKind.DOUBLE -> "getDoubleArray()"
          TypeKind.DECLARED -> {
            componentType as DeclaredType
            if (componentType.asTypeName() == STRING) {
              "getStringArray()"
            } else {
              throw UnsupportedOperationException("Unsupported array type: ${element.asType()}")
            }
          }
          else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
        }
      }
      TypeKind.DECLARED -> {
        val declaredType = element.asType() as DeclaredType
        if (typeUtils.isSameType(declaredType, elementUtils.getTypeElement(String::class.java.name).asType())) {
          "readString()"
        } else if (declaredType.asElement().javaToKotlinType() == List::class.asTypeName()) {
          val typeMirror = declaredType.typeArguments[0]
          when (typeMirror.kind) {
            TypeKind.BYTE -> "getByteList()"
            TypeKind.INT -> "getIntList()"
            TypeKind.LONG -> "getLongList()"
            TypeKind.DECLARED -> "getStringList()"
            else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
          }
        } else {
          "decodeBytesAs(${declaredType.toClassName().canonicalName}::class.java)"
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
    val invokerClassName = getInvokerClassName(typeElement)
    return ControllerTypeElement(moduleId, typeElement, cmdMethods, cmdVariables, invokerClassName)
  }

  private data class ControllerTypeElement(
    val moduleId: Short,
    val typeElement: TypeElement,
    val cmdMethods: List<CmdExecutableElement>,
    val cmdVariables: List<CmdVariableElement>,
    val invokerClassName: ClassName
  ) {
    val invokerVarName = invokerClassName.simpleName.uncapitalize()
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
