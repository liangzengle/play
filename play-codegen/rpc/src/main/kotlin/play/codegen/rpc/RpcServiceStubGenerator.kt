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
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import play.codegen.*
import play.codegen.ksp.*

@AutoService(SymbolProcessorProvider::class)
class RpcServiceStubGeneratorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return RpcServiceStubGenerator(environment)
  }
}

class RpcServiceStubGenerator(environment: SymbolProcessorEnvironment) :
  AbstractSymbolProcessor(environment) {
  override fun process(): List<KSAnnotated> {
    resolver.getClassesAnnotatedWith(RpcServiceInterface)
      .forEach { ksClass ->
        val generateStub = ksClass.getAnnotation(RpcServiceInterface).getValue<Boolean>("generateStub")
        if (generateStub) {
          generate(ksClass)
        }
      }
    return emptyList()
  }

  private fun generate(rpcServiceInterfaceDeclaration: KSClassDeclaration) {
    val rpcServiceClassName = rpcServiceInterfaceDeclaration.toClassName()
    val builder =
      TypeSpec.classBuilder(rpcServiceClassName.simpleName + "Stub")
        .superclass(RpcServiceStub)
        .addSuperinterface(rpcServiceInterfaceDeclaration.toClassName())
    builder.primaryConstructor(
      FunSpec.constructorBuilder()
        .addParameter("requester", AbstractRSocketRequester)
        .addParameter("ioStreamAdapter", ByteBufToIOStreamAdapter)
        .addParameter("serializerProvider", RSocketSerializerProvider)
        .build()
    ).addProperty(
      PropertySpec.builder("requester", AbstractRSocketRequester).initializer("requester").build()
    ).addProperty(
      PropertySpec.builder("ioStreamAdapter", ByteBufToIOStreamAdapter).initializer("ioStreamAdapter").build()
    ).addProperty(
      PropertySpec.builder("serializerProvider", RSocketSerializerProvider).initializer("serializerProvider").build()
    )

    builder.addFunction(
      FunSpec.builder("serviceInterface").returns(Class::class.asClassName().parameterizedBy(STAR))
        .addModifiers(KModifier.OVERRIDE).addStatement("return %T::class.java", rpcServiceClassName).build()
    )

    val companionObjectBuilder = TypeSpec.companionObjectBuilder()


    for (function in rpcServiceInterfaceDeclaration.getDeclaredFunctions()) {
      if (!function.isAbstract) {
        continue
      }
      val functionName = function.simpleName.asString()
      val funSpec = implementFunction(function)
      builder.addFunction(funSpec)

      companionObjectBuilder.addProperty(
        PropertySpec.builder(functionName, RpcMethodMetadata)
          .initializer("%T.of(%T::%L)", RpcMethodMetadata, rpcServiceClassName, functionName)
          .build()
      )
    }
    builder.addType(companionObjectBuilder.build())

    write(builder.build(), rpcServiceInterfaceDeclaration.packageName.asString())
  }

  private fun implementFunction(functionDeclaration: KSFunctionDeclaration): FunSpec {
    val functionName = functionDeclaration.simpleName.asString()
    val builder = FunSpec.builder(functionName)
    val parameters = functionDeclaration.parameters
    functionDeclaration.returnType?.also { builder.returns(it.toTypeName2()) }
    builder.addParameters(parameters.map(::toParameterSpec))
    builder.addModifiers(functionDeclaration.modifiers.mapNotNull { it.toKModifier() })
    builder.addModifiers(KModifier.OVERRIDE)

    if (parameters.isNotEmpty()) {
      builder.addStatement("val serializer = serializerProvider.get()")
    }

    builder.addStatement("var publisher: %T<*>? = null", Flux)
    builder.addStatement("val buffer = %T.DEFAULT.buffer(64)", ByteBufAllocator)
    builder.addStatement("buffer.writeInt(%L.serviceId)", functionName)
    builder.addStatement("buffer.writeInt(%L.methodId)", functionName)
    builder.addStatement("val output = ioStreamAdapter.toOutputStream(buffer)")
    for (parameter in parameters) {
      val parameterType = parameter.type.resolve()
      val parameterName = parameter.name!!.asString()
      if (parameterType.toClassName() == Flux) {
        builder.addStatement("publisher = %L", parameterName)
      } else {
        val writer = getWriter(parameterType, parameterName)
        builder.addStatement("%L", writer)
      }
    }
    val returnType = functionDeclaration.returnType?.resolve()
    val hasReturn = returnType != null && returnType != resolver.builtIns.unitType
    if (hasReturn) {
      builder.addCode(
        "return requester.initiateRequest(routingMetadata, %L, buffer, publisher)",
        functionName
      )
    } else {
      builder.addCode(
        "requester.initiateRequest<Any?>(routingMetadata, %L, buffer, publisher)",
        functionName
      )
    }
    return builder.build()
  }

  private fun getWriter(ksType: KSType, name: String): String {
    // primitive and string
    if (ksType == resolver.builtIns.intType) {
      return "serializer.writeInt(output, $name)"
    }
    if (ksType == resolver.builtIns.longType) {
      return "serializer.writeLong(output, $name)"
    }
    if (ksType == resolver.builtIns.booleanType) {
      return "serializer.writeBoolean(output, $name)"
    }
    if (ksType == resolver.builtIns.doubleType) {
      return "serializer.writeDouble(output, $name)"
    }
    if (ksType == resolver.builtIns.stringType) {
      return if (ksType.nullability == Nullability.NULLABLE) "serializer.writeUtf8(output, $name)"
      else """serializer.writeUtf8(output, $name)"""
    }
    if (ksType == resolver.builtIns.byteType) {
      return "serializer.writeByte(output, $name)"
    }
    if (ksType == resolver.builtIns.shortType) {
      return "serializer.writeShort(output, $name)"
    }
    if (ksType == resolver.builtIns.floatType) {
      return "serializer.writeFloat(output, $name)"
    }
    val typeName = ksType.toTypeName()
    if (typeName == INT_ARRAY) {
      return "serializer.writeInts(output, $name)"
    }
    if (typeName == LONG_ARRAY) {
      return "serializer.writeLongs(output, $name)"
    }
    if (typeName == BOOLEAN_ARRAY) {
      return "serializer.writeBooleans(output, $name)"
    }
    if (typeName == DOUBLE_ARRAY) {
      return "serializer.writeDoubles(output, $name)"
    }
    if (typeName == BYTE_ARRAY) {
      return "serializer.writeBytes(output, $name)"
    }
    if (typeName == SHORT_ARRAY) {
      return "serializer.writeShorts(output, $name)"
    }
    if (typeName == FLOAT_ARRAY) {
      return "serializer.writeFloats(output, $name)"
    }
    // object
    return CodeBlock.builder()
      .apply {
        if (ksType.nullability == Nullability.NULLABLE) {
          addStatement("serializer.writeObjectOrNull(output, %M<%T>(), $name)", typeOf, typeName)
        } else {
          addStatement("serializer.writeObject(output, %M<%T>(), $name)", typeOf, typeName)
        }
      }
      .build().toString()
  }
}
