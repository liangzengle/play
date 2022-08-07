package play.codegen.rpc

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import play.codegen.*
import play.codegen.ksp.*

@AutoService(SymbolProcessorProvider::class)
class RpcServiceCallerGeneratorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return RpcServiceInvokerGenerator(environment)
  }
}

class RpcServiceInvokerGenerator(environment: SymbolProcessorEnvironment) :
  AbstractSymbolProcessor(environment) {
  override fun process(): List<KSAnnotated> {
    resolver.getClassesAnnotatedWith(RpcServiceImplementation)
      .forEach {
        val serviceInterface =
          it.getAnnotation(RpcServiceImplementation).getValue<KSType>("value").declaration as KSClassDeclaration
        generate(serviceInterface, it)
      }
    return emptyList()
  }

  private fun generate(rpcServiceInterfaceDeclaration: KSClassDeclaration, implDeclaration: KSClassDeclaration) {
    val rpcServiceClassName = rpcServiceInterfaceDeclaration.toClassName()
    val builder =
      TypeSpec.classBuilder(rpcServiceClassName.simpleName + "Caller")
        .addSuperinterface(LocalServiceCaller)
        .addAnnotations(guessIocSingletonAnnotations(resolver))
    builder.primaryConstructor(
      FunSpec.constructorBuilder()
        .addParameter("underlying", rpcServiceClassName)
        .addParameter("ioStreamAdapter", ByteBufToIOStreamAdapter)
        .addParameter("serializerProvider", PlaySerializerProvider)
        .build()
    ).addProperty(
      PropertySpec.builder("underlying", rpcServiceClassName).initializer("underlying").build()
    ).addProperty(
      PropertySpec.builder("ioStreamAdapter", ByteBufToIOStreamAdapter).initializer("ioStreamAdapter").build()
    ).addProperty(
      PropertySpec.builder("serializerProvider", PlaySerializerProvider).initializer("serializerProvider").build()
    )

    builder.addFunction(
      FunSpec.builder("serviceInterface").returns(Class::class.asClassName().parameterizedBy(STAR))
        .addModifiers(KModifier.OVERRIDE).addStatement("return %T::class.java", rpcServiceClassName).build()
    )

    val invokeBuilder = FunSpec.builder("call")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("methodId", INT)
      .addParameter("data", ByteBuf)
      .addParameter("publisher", Flux.parameterizedBy(Payload).copy(nullable = true))
      .returns(
        Any::class.asClassName()
          .copy(nullable = true)
      )
    invokeBuilder.beginControlFlow("return when(methodId)")

    val companionObjectBuilder = TypeSpec.companionObjectBuilder()


    for (function in rpcServiceInterfaceDeclaration.getDeclaredFunctions()) {
      if (!function.isAbstract) {
        continue
      }
      val functionName = function.simpleName.asString()
      val funSpec = toInvokerFunction(function)
      builder.addFunction(funSpec)
      invokeBuilder.addStatement("%L -> %L(data, publisher)", functionName, functionName)

      val methodId = function.getAnnotationOrNull(RpcMethod)?.getValue<Int>("value") ?: Murmur3.hash32(functionName)
      companionObjectBuilder.addProperty(
        PropertySpec.builder(functionName, INT)
          .addModifiers(KModifier.CONST)
          .initializer("$methodId")
          .build()
      )
    }
    invokeBuilder.addStatement("else -> %T.NotFound", LocalServiceCaller)
    invokeBuilder.endControlFlow()

    builder.addFunction(invokeBuilder.build())
    builder.addType(companionObjectBuilder.build())

    write(builder.build(), implDeclaration.packageName.asString())
  }

  private fun toInvokerFunction(functionDeclaration: KSFunctionDeclaration): FunSpec {
    val functionName = functionDeclaration.simpleName.asString()
    val parameters = functionDeclaration.parameters

    val builder = FunSpec.builder(functionName)
    builder.addParameter("data", ByteBuf)
    builder.addParameter("_publisher", Flux.parameterizedBy(Payload).copy(nullable = true))

    if (parameters.isNotEmpty()) {
      builder.addStatement("val serializer = serializerProvider.get()")
    }
    builder.addStatement("val input = ioStreamAdapter.toInputStream(data)")
    for (parameter in parameters) {
      val parameterType = parameter.type.resolve()
      val parameterName = parameter.name!!.asString()
      if (parameterType.toClassName() == Flux) {
        val t = parameterType.toTypeName()
        t as ParameterizedTypeName
        val typeArg = parameterType.arguments[0].type!!.resolve()
        val reader = getReader(typeArg, "ioStreamAdapter.toInputStream(payload.data())")
        builder.beginControlFlow("val %L = _publisher!!.map { payload ->", parameterName)
        builder.addStatement(reader)
        builder.endControlFlow()
      } else {
        val reader = getReader(parameterType, "input")
        builder.addStatement("val %L = %L", parameterName, reader)
      }
    }
    val returnType = functionDeclaration.returnType?.resolve()
    returnType?.also { builder.returns(it.toTypeName()) }
    if (returnType != null && returnType != resolver.builtIns.unitType) {
      builder.addCode("return ")
    }
    builder.addStatement("underlying.%L(%L)", functionName, toParamString(functionDeclaration.parameters))
    return builder.build()
  }

  private fun getReader(ksType: KSType, inputVarName: String): String {
    // primitive and string
    if (ksType == resolver.builtIns.intType) {
      return "serializer.readInt($inputVarName)"
    }
    if (ksType == resolver.builtIns.longType) {
      return "serializer.readLong($inputVarName)"
    }
    if (ksType == resolver.builtIns.booleanType) {
      return "serializer.readBoolean($inputVarName)"
    }
    if (ksType == resolver.builtIns.doubleType) {
      return "serializer.readDouble($inputVarName)"
    }
    if (ksType == resolver.builtIns.stringType) {
      return if (ksType.nullability == Nullability.NULLABLE) "serializer.readUtf8($inputVarName)"
      else """serializer.readUtf8($inputVarName) ?: """""
    }
    if (ksType == resolver.builtIns.byteType) {
      return "serializer.readByte($inputVarName)"
    }
    if (ksType == resolver.builtIns.shortType) {
      return "serializer.readShort($inputVarName)"
    }
    if (ksType == resolver.builtIns.floatType) {
      return "serializer.readFloat($inputVarName)"
    }
    val typeName = ksType.toTypeName()
    if (typeName == INT_ARRAY) {
      return "serializer.readInts($inputVarName)"
    }
    if (typeName == LONG_ARRAY) {
      return "serializer.readLongs($inputVarName)"
    }
    if (typeName == BOOLEAN_ARRAY) {
      return "serializer.readBooleans($inputVarName)"
    }
    if (typeName == DOUBLE_ARRAY) {
      return "serializer.readDoubles($inputVarName)"
    }
    if (typeName == BYTE_ARRAY) {
      return "serializer.readBytes($inputVarName)"
    }
    if (typeName == SHORT_ARRAY) {
      return "serializer.readShorts($inputVarName)"
    }
    if (typeName == FLOAT_ARRAY) {
      return "serializer.readFloats($inputVarName)"
    }
    // object
    return CodeBlock.builder()
      .apply {
        if (ksType.nullability == Nullability.NULLABLE) {
          addStatement("serializer.readObjectOrNull<%T>($inputVarName, %M<%T>())", typeName, typeOf, typeName)
        } else {
          addStatement("serializer.readObject<%T>($inputVarName, %M<%T>())", typeName, typeOf, typeName)
        }
      }
      .build().toString()
  }
}
