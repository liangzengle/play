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

  private lateinit var useClass: ClassName

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    processingEnv.options["controller.user-class"]?.also {
      useClass = ClassName.bestGuess(it)
    }
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
    val self = useClass
    val invoke1 = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request)
    val handlerBuilder = StringBuilder(2048)
    handlerBuilder.append("return when(request.header.msgId.moduleId.toInt()) {\n")
    controllers.forEach { c ->
      handlerBuilder.append(c.moduleId).append('-').append('>').append(' ')
        .append(c.simpleName.toString().decapitalize())
        .append(".invoke(self, request)\n")
    }
    handlerBuilder.append("else -> null\n")
    handlerBuilder.append("}\n")
    invoke1.addStatement(handlerBuilder.toString())

    classBuilder.addFunction(invoke1.build())

    if (useClass != Long::class.java.asTypeName()) {
      val invoke2 = FunSpec.builder("invoke")
        .addParameter("playerId", Long::class)
        .addParameter("request", Request)
        .returns(RequestResult.parameterizedBy(STAR).copy(true))
      val handlerBuilder2 = StringBuilder(2048)
      handlerBuilder2.append("return when(request.header.msgId.moduleId.toInt()) {\n")
      controllers.asSequence()
        .filter { c ->
          c.typeElement.enclosedElements.any { member -> member.isAnnotationPresent(NotPlayerThread) }
        }
        .forEach { c ->
          handlerBuilder2.append(c.moduleId).append('-').append('>').append(' ')
            .append(c.simpleName.toString().decapitalize())
            .append(".invoke(playerId, request)\n")
        }
      handlerBuilder2.append("else -> null\n")
      handlerBuilder2.append("}\n")
      invoke2.addStatement(handlerBuilder2.toString())

      classBuilder.addFunction(invoke2.build())
    }


    val format = FunSpec.builder("format")
      .addParameter("request", Request)
    val formatBuilder = StringBuilder(2048)
    formatBuilder.append("return when(request.header.msgId.moduleId.toInt()) {\n")
    controllers.forEach { c ->
      formatBuilder.append(c.moduleId).append('-').append('>').append(' ')
        .append(c.simpleName.toString().decapitalize())
        .append(".format(request)\n")
    }
    formatBuilder.append("else -> request.toString()\n")
    formatBuilder.append("}\n")
    format.addStatement(formatBuilder.toString())

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

    classBuilder.addInitializerBlock(
      CodeBlock.of(
        """
        if (controller.moduleId != MODULE_ID) {
          throw IllegalStateException("${typeElement.simpleName}的模块id不一致")
        }
        """.trimIndent()
      )
    )
    classBuilder.addProperty(moduleId)
    classBuilder.addFunction(genInvoker(typeElement))
    if (useClass != Long::class.java.asTypeName()) {
      classBuilder.addFunction(genInvoke2(typeElement))
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
    val format = FunSpec.builder("format")
      .addParameter("request", Request)
      .returns(String::class)

    val self = useClass
    val b = StringBuilder(512)
    b.append("request.body.reset()\n")
    b.append("return when(request.header.msgId.cmd.toInt()) {\n")
    typeElement.cmdMethods.forEach { elem ->
      val cmd = elem.cmd
      b.append(cmd).append(" -> \n")
      b.append('\"')
        .append("\$moduleId, $cmd: ")
        .append(typeElement.simpleName).append(".").append(elem.simpleName).append('(')
      var first = true
      elem.parameters.forEach { p ->
        if (!first) {
          b.append(", ")
          first = false
        }
        val pTypeName = p.asType().asTypeName()
        if (pTypeName != self && pTypeName != Request) {
          val reader = readStmt(p)
          val toString = if (p.asType().kind == TypeKind.ARRAY) "$reader.contentToString()" else reader
          b.append("\${").append(toString).append('}')
        }
      }
      b.append('\"').append('\n')
    }
    b.append("\n else -> request.toString()\n")
    b.append('}').append('\n')
    format.addStatement(b.toString())
    return format.build()
  }

  private fun genInvoker(typeElement: ControllerTypeElement): FunSpec {
    val self = useClass
    val handle = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request)
      .returns(RequestResult.parameterizedBy(STAR).copy(true))

    val body = typeElement.cmdMethods
      .asSequence()
      .filterNot { it.isAnnotationPresent(NotPlayerThread) }
      .map { elem ->
        val cmd = elem.cmd
        val dummy = elem.dummy
        val b = StringBuilder()
        if (dummy) {
          b.append(cmd).append(" -> null")
        } else {
          val controllerFunc = elem as ExecutableElement
          b.append(cmd).append(" -> ").append("{\n")
          controllerFunc.parameters
            .filterNot { p ->
              (p.simpleName.contentEquals("playerId") && p.asType().asTypeName() == Long::class.asTypeName()) ||
                p.asType().asTypeName() == self || p.asType().asTypeName() == Request
            }
            .forEach {
              b.append("val ").append(it.simpleName).append(" = ").append(readStmt(it)).append("\n")
            }
          b.append("controller.").append(controllerFunc.simpleName).append('(')
          b.append(controllerFunc.parameters.joinToString(", ") { it.simpleName })
          b.append(')')
          b.append("\n").append("    }")
          b.toString()
        }
      }.joinToString("\n")

    handle.addStatement(
      """
           request.body.reset()
           return when(request.header.msgId.cmd.toInt()) {
              $body
              else -> null
            }
          """.trimIndent()
    )
    return handle.build()
  }

  private fun genInvoke2(typeElement: ControllerTypeElement): FunSpec {
    val func = FunSpec.builder("invoke")
      .addParameter("playerId", Long::class)
      .addParameter("request", Request)
      .returns(RequestResult.parameterizedBy(STAR).copy(true))

    val body = typeElement.cmdMethods
      .asSequence()
      .filter { it.isAnnotationPresent(NotPlayerThread) }
      .map { elem ->
        val cmd = getAnnotationValue(elem, Cmd, "value", 0.toByte())
        val dummy = getAnnotationValue(elem, Cmd, "dummy", false)
        val b = StringBuilder()
        if (dummy) {
          b.append(cmd).append(" -> null")
        } else {
          val controllerFunc = elem as ExecutableElement
          b.append(cmd).append(" -> ").append("{\n")
          controllerFunc.parameters
            .filterNot { p ->
              (p.simpleName.contentEquals("playerId") &&
                p.asType().asTypeName() == Long::class.asTypeName()) || p.asType().asTypeName() == Request
            }
            .forEach {
              b.append("val ").append(it.simpleName).append(" = ").append(readStmt(it)).append("\n")
            }
          b.append("controller.").append(controllerFunc.simpleName).append('(')
          b.append(controllerFunc.parameters.joinToString(", ") { it.simpleName })
          b.append(')')
          b.append("\n").append("    }")
          b.toString()
        }
      }.joinToString("\n")
    func.addCode(
      """
       return when(request.header.msgId.cmd.toInt()) {
              $body
              else -> null
            } 
      """.trimIndent()
    )
    return func.build()
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
        } else {
          val typeMirror = declaredType.typeArguments[0]
          when (typeMirror.kind) {
            TypeKind.BYTE -> "readByteList()"
            TypeKind.INT -> "readIntList()"
            TypeKind.LONG -> "readLongList()"
            TypeKind.DECLARED -> "readStringList()"
            else -> throw UnsupportedOperationException("Unsupported parameter type: ${element.asType()}")
          }
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
