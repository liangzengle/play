package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import play.mvc.*
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.inject.Singleton
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import kotlin.reflect.KClass

@AutoService(Processor::class)
class ControllerAnnotationProcessor : PlayAnnotationProcessor() {

  private lateinit var useClass: ClassName

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    processingEnv.options["controller.user-class"]?.also {
      useClass = ClassName.bestGuess(it)
    }
  }

  override fun getSupportedAnnotationTypes0(): Set<KClass<out Annotation>> {
    return setOf(Controller::class)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val controllers = roundEnv.subtypesOf(AbstractController::class).toList()
    if (controllers.isEmpty()) {
      return false
    }
    controllers.forEach(::generate)
    val orderedControllers = controllers.sortedBy { getAnnotationValue(it, Controller::class, "moduleId", 0.toShort()) }
    genInvokerManager(orderedControllers)
    return true
  }

  private fun genInvokerManager(controllers: List<TypeElement>) {
    val ctor = FunSpec.constructorBuilder().addAnnotation(Inject::class)
    controllers.forEach { c ->
      ctor.addParameter(c.simpleName.toString().decapitalize(), getInvokerClassName(c))
    }
    val className = "ControllerInvokerManager"
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton::class)
      .primaryConstructor(ctor.build())
    controllers.forEach { c ->
      classBuilder.addProperty(
        PropertySpec.builder(c.simpleName.toString().decapitalize(), getInvokerClassName(c), KModifier.PRIVATE)
          .initializer(c.simpleName.toString().decapitalize())
          .build()
      )
    }
    val self = useClass
    val invoke1 = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request::class.asClassName())
    val handlerBuilder = StringBuilder(2048)
    handlerBuilder.append("return when(request.header.msgId.moduleId.toInt()) {\n")
    controllers.forEach { c ->
      val moduleId = getAnnotationValue(c, Controller::class, "moduleId", 0.toShort())
      handlerBuilder.append(moduleId).append('-').append('>').append(' ').append(c.simpleName.toString().decapitalize())
        .append(".invoke(self, request)\n")
    }
    handlerBuilder.append("else -> null\n")
    handlerBuilder.append("}\n")
    invoke1.addStatement(handlerBuilder.toString())

    classBuilder.addFunction(invoke1.build())

    if (useClass != Long::class.java.asTypeName()) {
      val invoke2 = FunSpec.builder("invoke")
        .addParameter("playerId", Long::class.java)
        .addParameter("request", Request::class.asClassName())
        .returns(RequestResult::class.asTypeName().parameterizedBy(STAR).copy(true))
      val handlerBuilder2 = StringBuilder(2048)
      handlerBuilder2.append("return when(request.header.msgId.moduleId.toInt()) {\n")
      controllers.asSequence()
        .filter { clazz ->
          elementUtils.getAllMembers(clazz).any { member -> member.isAnnotationPresent(NotPlayerThread::class) }
        }
        .forEach { c ->
          val moduleId = getAnnotationValue(c, Controller::class, "moduleId", 0.toShort())
          handlerBuilder2.append(moduleId).append('-').append('>').append(' ')
            .append(c.simpleName.toString().decapitalize())
            .append(".invoke(playerId, request)\n")
        }
      handlerBuilder2.append("else -> null\n")
      handlerBuilder2.append("}\n")
      invoke2.addStatement(handlerBuilder2.toString())

      classBuilder.addFunction(invoke2.build())
    }


    val format = FunSpec.builder("format")
      .addParameter("request", Request::class.asClassName())
    val formatBuilder = StringBuilder(2048)
    formatBuilder.append("return when(request.header.msgId.moduleId.toInt()) {\n")
    controllers.forEach { c ->
      val moduleId = getAnnotationValue(c, Controller::class, "moduleId", 0.toShort())
      formatBuilder.append(moduleId).append('-').append('>').append(' ').append(c.simpleName.toString().decapitalize())
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

  private fun generate(typeElement: TypeElement) {
    val pkg = typeElement.getPackage()
    val className = getInvokerClassName(typeElement)
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton::class)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject::class)
          .addParameter("controller", typeElement.asClassName())
          .build()
      )
      .addProperty(
        PropertySpec.builder("controller", typeElement.asClassName(), KModifier.PRIVATE)
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

  private fun genCompanionObject(typeElement: TypeElement): TypeSpec {
    val moduleId = getAnnotationValue(typeElement, Controller::class, "moduleId", 0.toShort())
    val builder = TypeSpec.companionObjectBuilder()
    builder.addProperty(
      PropertySpec.builder("MODULE_ID", Short::class, KModifier.CONST)
        .initializer("%L", moduleId)
        .build()
    )

    typeElement.enclosedElements
      .asSequence()
      .filter { it.isAnnotationPresent(Cmd::class) }
      .forEach { elem ->
        val cmd = getAnnotationValue<Byte>(elem, Cmd::class, "value", 0)
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

  private fun genFormat(typeElement: TypeElement): FunSpec {
    val format = FunSpec.builder("format")
      .addParameter("request", Request::class.asClassName())
      .returns(String::class)

    val self = useClass
    val b = StringBuilder(512)
    b.append("request.body.reset()\n")
    b.append("return when(request.header.msgId.cmd.toInt()) {\n")
    ElementFilter.methodsIn(typeElement.enclosedElements)
      .asSequence()
      .filter(::isCmdFunction)
      .forEach { elem ->
        val cmd = getAnnotationValue<Byte>(elem, Cmd::class, "value", 0)
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
          if (p.asType().asTypeName() == self) {
          } else if (p.asType() == Request::class.asTypeMirror()) {

          } else {
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

  private fun genInvoker(typeElement: TypeElement): FunSpec {
    val self = useClass
    val handle = FunSpec.builder("invoke")
      .addParameter("self", self)
      .addParameter("request", Request::class.asClassName())
      .returns(RequestResult::class.asTypeName().parameterizedBy(STAR).copy(true))

    val body = elementUtils.getAllMembers(typeElement)
      .asSequence()
      .filter(::isCmdFunction)
      .filterNot { it.isAnnotationPresent(NotPlayerThread::class) }
      .map { elem ->
        val cmd = getAnnotationValue(elem, Cmd::class, "value", 0.toByte())
        val dummy = getAnnotationValue(elem, Cmd::class, "dummy", false)
        val b = StringBuilder()
        if (dummy) {
          b.append(cmd).append(" -> null")
        } else {
          val controllerFunc = elem as ExecutableElement
          b.append(cmd).append(" -> ").append("{\n")
          controllerFunc.parameters
            .filterNot { p ->
              (p.simpleName.contentEquals("playerId") && p.asType().asTypeName() == Long::class.asTypeName()) ||
                p.asType().asTypeName() == self || p.asType() == Request::class.asTypeMirror()
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

  private fun genInvoke2(typeElement: TypeElement): FunSpec {
    val func = FunSpec.builder("invoke")
      .addParameter("playerId", Long::class)
      .addParameter("request", Request::class)
      .returns(RequestResult::class.asTypeName().parameterizedBy(STAR).copy(true))

    val body = elementUtils.getAllMembers(typeElement)
      .asSequence()
      .filter(::isCmdFunction)
      .filter { it.isAnnotationPresent(NotPlayerThread::class) }
      .map { elem ->
        val cmd = getAnnotationValue(elem, Cmd::class, "value", 0.toByte())
        val dummy = getAnnotationValue(elem, Cmd::class, "dummy", false)
        val b = StringBuilder()
        if (dummy) {
          b.append(cmd).append(" -> null")
        } else {
          val controllerFunc = elem as ExecutableElement
          b.append(cmd).append(" -> ").append("{\n")
          controllerFunc.parameters
            .filterNot { p ->
              (p.simpleName.contentEquals("playerId") && p.asType()
                .asTypeName() == Long::class.asTypeName()) || p.asType() == Request::class.asTypeMirror()
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

  private fun isCmdFunction(element: Element): Boolean {
    return element is ExecutableElement && element.isAnnotationPresent(Cmd::class)
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
        if (declaredType.asTypeName() == String::class.java.asTypeName()) {
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
}
