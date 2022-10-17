package play.example.robot.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.MsgId
import play.mvc.Push
import play.util.concurrent.CommonPool
import play.util.reflect.ClassgraphClassScanner
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

@OptIn(DelicateKotlinPoetApi::class)
object Generator {
  @JvmStatic
  fun main(args: Array<String>) {
    val dir = if (args.isEmpty()) null else args[0].replace('.', '/')
    println(dir)

    val dispatcher = TypeSpec.classBuilder("ResponseDispatcher")
    dispatcher.addAnnotation(Component::class)
    val ctor = FunSpec.constructorBuilder().addAnnotation(Autowired::class)
    val dispatch = FunSpec.builder("dispatch")
      .addParameter("ctx", Types.ChannelHandlerContext)
      .addParameter("response", Types.Response)
    val dispatchCodeBlock = CodeBlock.builder()
    dispatchCodeBlock.beginControlFlow("when(response.header.msgId.toInt())")
    val autowiredNotRequired = AnnotationSpec.builder(Autowired::class).addMember("%L", "required = false").build()
    ClassgraphClassScanner(CommonPool, emptyList(), listOf("play.example")).scanResult.use {
      it.getClassesWithAnnotation(Controller::class.java.name)
        .forEach { classInfo ->
          val controller = classInfo.getAnnotationInfo(Controller::class.java.name)
          val moduleId = controller.parameterValues.getValue("moduleId") as Short
          val clazz = classInfo.loadClass()
          val className = ClassName("play.example.robot.module", getModuleName(clazz) + "Module")
          generate(
            dir,
            className,
            clazz,
            moduleId,
            dispatchCodeBlock
          )
          val propertyName = className.simpleName.replaceFirstChar { c -> c.lowercaseChar() }
          ctor.addParameter(
            ParameterSpec.builder(propertyName, className.copy(true)).addAnnotation(autowiredNotRequired).build()
          )
          dispatcher.addProperty(
            PropertySpec.builder(propertyName, className.copy(true)).initializer(propertyName).build()
          )
        }
    }
    dispatchCodeBlock.addStatement("""else -> println("Unhandled response: ${'$'}response")""")
    dispatchCodeBlock.endControlFlow()
    dispatch.addCode(dispatchCodeBlock.build())
    val typeSpec = dispatcher.primaryConstructor(ctor.build()).addFunction(dispatch.build()).build()

    if (dir != null) {
      FileSpec.builder("play.example.robot.net", "ResponseDispatcher")
        .addType(typeSpec)
        .build()
        .writeTo(File(dir))
    }
  }

  private fun generate(
    dir: String?,
    className: ClassName,
    clazz: Class<*>,
    moduleId: Short,
    dispatcher: CodeBlock.Builder
  ) {
    val typeSpecList = arrayListOf<TypeSpec>()
    val funSpecList = arrayListOf<FunSpec>()
    for (method in clazz.methods) {
      val cmd = method.getAnnotation(Cmd::class.java) ?: continue
      val req = FunSpec.builder("${method.name}Req")
      req.addParameter("player", Types.RobotPlayer)
      val paramsClassName = ClassName(
        className.packageName,
        listOf(className.simpleName, "${method.name.replaceFirstChar { it.uppercaseChar() }}RequestParams")
      )
      val paramsClassBuilder = TypeSpec.classBuilder(paramsClassName).superclass(Types.RequestParams)
      val ctor = FunSpec.constructorBuilder()
      val toRequestBody = FunSpec.builder("toRequestBody").addModifiers(KModifier.OVERRIDE)
        .returns(Types.RequestBody)
      val toRequestBodyCodeBlock = CodeBlock.builder()
      toRequestBodyCodeBlock.add("return %T.builder()", Types.RequestBodyFactory)
      val reqCodeBlock = CodeBlock.builder()
      reqCodeBlock.add("player.send(%T($moduleId.toShort(), ${cmd.value}.toByte()).toInt()", Types.MsgId)
      for (parameter in method.parameters) {
        if (parameter.type == Self::class.java) {
          continue
        }
        val parameterType = parameter.parameterizedType.javaToKotlinType()
        req.addParameter(parameter.name, parameterType)

        ctor.addParameter(parameter.name, parameterType)
        val propertySpec = PropertySpec.builder(parameter.name, parameterType).initializer(parameter.name).build()
        paramsClassBuilder.addProperty(propertySpec)

        toRequestBodyCodeBlock.addStatement(".write(${parameter.name})")
      }
      val requestParamsClassName: ClassName
      if (ctor.parameters.size > 0) {
        toRequestBodyCodeBlock.addStatement(".build()").build()
        paramsClassBuilder.primaryConstructor(ctor.build())
        paramsClassBuilder.addFunction(toRequestBody.addCode(toRequestBodyCodeBlock.build()).build())
        typeSpecList.add(paramsClassBuilder.build())

        reqCodeBlock.add(", %L(", paramsClassName.simpleName)
        var first = true
        for (parameter in ctor.parameters) {
          if (!first) {
            reqCodeBlock.add(", ")
          }
          reqCodeBlock.add(parameter.name)
          first = false
        }
        reqCodeBlock.add(")")
        requestParamsClassName = ClassName.bestGuess(paramsClassName.simpleName)
      } else {
        reqCodeBlock.add(", null")
        requestParamsClassName = ANY
      }
      reqCodeBlock.add(")")
      req.addCode(reqCodeBlock.build())

      val returnType = getReturnType(method)
      val respFunName = "${method.name}Resp".replaceFirstChar { it.lowercaseChar() }
      val respErrorFunName = "${method.name}Error"
      val respRaw = FunSpec.builder(respFunName)
        .addParameter("ctx", Types.ChannelHandlerContext)
        .addParameter("response", Types.Response)
        .addCode(
          CodeBlock.builder()
            .addStatement("val player = ctx.channel().attr(%T.AttrKey).get()", Types.RobotPlayer)
            .beginControlFlow("player.execute")
            .addStatement("val requestId = response.header.requestId")
            .addStatement("val req = player.getRequestParams(requestId)")
            .addStatement("val statusCode = response.statusCode")
            .beginControlFlow("if (statusCode == 0)")
            .apply {
              if (returnType == ANY) addStatement("val data = %M", Types.EmptyByteArray)
              else addStatement("val data = %T.decode(response.body, %T::class)", Types.MessageCodec, returnType)
            }
            .addStatement("%L(player, data, req as? %L)", respFunName, requestParamsClassName)
            .nextControlFlow("else")
            .apply {
              if (returnType == ANY) addStatement("val data = %M", Types.EmptyByteArray)
              else addStatement(
                "val data = if(response.body.isEmpty()) null else %T.decode(response.body, %T::class)",
                Types.MessageCodec,
                returnType
              )
            }
            .addStatement("%L(player, statusCode, data, req as? %L)", respErrorFunName, requestParamsClassName)
            .endControlFlow()
            .endControlFlow()
            .build()
        )
      val errorResp = FunSpec.builder(respErrorFunName)
        .addModifiers(KModifier.OPEN)
        .addParameter("player", Types.RobotPlayer)
        .addParameter(ParameterSpec("statusCode", INT))
        .addParameter("data", returnType.copy(true))
        .addParameter(ParameterSpec("req", requestParamsClassName.copy(true)))
        .addCode("""System.err.println("$respErrorFunName error: ${'$'}statusCode")""")

      val resp = FunSpec.builder(respFunName).addModifiers(KModifier.ABSTRACT)
        .addParameter("player", Types.RobotPlayer)
        .addParameter("data", returnType)
        .addParameter(ParameterSpec("req", requestParamsClassName.copy(true)))

      funSpecList.add(req.build())
      funSpecList.add(respRaw.build())
      funSpecList.add(resp.build())
      funSpecList.add(errorResp.build())

      dispatcher.addStatement(
        "${MsgId(moduleId, cmd.value).toInt()} -> %L?.%L(ctx, response)",
        className.simpleName.replaceFirstChar { it.lowercaseChar() },
        respFunName
      )
    }

    for (field in clazz.fields) {
      if (field.type != Push::class.java) {
        continue
      }
      val cmd = field.getAnnotation(Cmd::class.java) ?: continue
      val returnType = getReturnType(field)
      val respFunName = "${field.name}Resp".replaceFirstChar { it.lowercaseChar() }
      val respRaw = FunSpec.builder(respFunName)
        .addParameter("ctx", Types.ChannelHandlerContext)
        .addParameter("response", Types.Response)
        .addCode(
          CodeBlock.builder()
            .addStatement("val player = ctx.channel().attr(%T.AttrKey).get()", Types.RobotPlayer)
            .beginControlFlow("player.execute")
            .addStatement("val requestId = response.header.requestId")
            .addStatement("val req = player.getRequestParams(requestId)")
            .addStatement("val data = %T.decode(response.body, %T::class)", Types.MessageCodec, returnType)
            .addStatement("%L(player, data, req)", respFunName)
            .endControlFlow()
            .build()
        )

      val funSpec = FunSpec.builder(respFunName)
        .addModifiers(KModifier.ABSTRACT)
        .addAnnotation(AnnotationSpec.get(cmd))
        .addParameter("player", Types.RobotPlayer)
        .addParameter("data", returnType)
        .addParameter(ParameterSpec("req", ANY.copy(true)))
        .build()
      funSpecList.add(funSpec)
      funSpecList.add(respRaw.build())

      dispatcher.addStatement(
        "${MsgId(moduleId, cmd.value).toInt()} -> %L?.%L(ctx, response)",
        className.simpleName.replaceFirstChar { it.lowercaseChar() },
        respFunName
      )
    }

    val typeSpec = TypeSpec
      .classBuilder(className.simpleName)
      .addModifiers(KModifier.ABSTRACT)
      .addTypes(typeSpecList)
      .addFunctions(funSpecList).build()

    if (dir != null) {
      FileSpec.builder(className.packageName, className.simpleName)
        .addType(typeSpec)
        .build()
        .writeTo(File(dir))
    }
  }

  private fun getModuleName(clazz: Class<*>): String {
    return clazz.simpleName.removeSuffix("Controller")
  }

  private fun getReturnType(method: Method): TypeName {
    val genericReturnType = method.genericReturnType
    if (genericReturnType !is ParameterizedType) {
      return ANY
    }
    val argType = genericReturnType.actualTypeArguments[0]
    if (argType == Unit::class.java || argType == Void::class.java) {
      return ANY
    }
    return genericReturnType.actualTypeArguments[0].javaToKotlinType()
  }

  private fun getReturnType(field: Field): TypeName {
    val genericType = field.genericType
    genericType as ParameterizedType
    return genericType.actualTypeArguments[0].javaToKotlinType()
  }

  private fun Type.javaToKotlinType(): TypeName {
    return when (this) {
      ByteArray::class.java -> BYTE_ARRAY
      IntArray::class.java -> INT_ARRAY
      LongArray::class.java -> LONG_ARRAY
      is Class<*> -> {
        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.canonicalName))?.asSingleFqName()
          ?.asString()?.let { ClassName.bestGuess(it) } ?: this.asTypeName()
      }

      is ParameterizedType -> {
        this.asTypeName().javaToKotlinType()
      }

      else -> this.asTypeName()
    }
  }

  private fun TypeName.javaToKotlinType(): TypeName {
    return when (this) {
      is ClassName -> {
        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.canonicalName))?.asSingleFqName()
          ?.asString()?.let { ClassName.bestGuess(it) } ?: this
      }

      is ParameterizedTypeName -> {
        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.rawType.canonicalName))?.asSingleFqName()
          ?.asString()?.let { ClassName.bestGuess(it) }?.parameterizedBy(this.typeArguments) ?: this
      }

      else -> this
    }
  }
}
