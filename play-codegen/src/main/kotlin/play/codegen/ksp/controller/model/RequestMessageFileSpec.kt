package play.codegen.ksp.controller.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import play.codegen.*
import play.codegen.ksp.controller.getModuleName
import play.codegen.ksp.controller.isRequestCommander
import play.codegen.ksp.controller.readStmt
import play.codegen.ksp.getAnnotation
import play.codegen.ksp.getValue
import play.codegen.ksp.toTypeName2

class RequestMessageFileSpec(private val controllerClass: ControllerClassDeclaration) {

  fun build(): FileSpec {
    val interfaceType =
      controllerClass.getAnnotation(GeneratePlayerRequestMessage).getValue<KSType>("interfaceType").toTypeName()
    val moduleName = getModuleName(controllerClass)
    val converterSimpleName = moduleName + "MessageConverter"
    val objectBuilder = TypeSpec.objectBuilder(converterSimpleName)
    objectBuilder.superclass(MessageConverter)
    val convert =
      FunSpec.builder("convert").addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE).addParameter("pr", PlayerRequest)
        .returns(interfaceType)
    val codeBlock = CodeBlock.builder()
    codeBlock.beginControlFlow("return when(pr.cmd.toInt())")

    val baseModuleMessage =
      TypeSpec.classBuilder(moduleName + "PlayerRequest").addModifiers(KModifier.PUBLIC, KModifier.OPEN)
        .superclass(AbstractPlayerRequest).addSuperinterface(interfaceType)
        .addSuperclassConstructorParameter("playerId, request").primaryConstructor(
          FunSpec.constructorBuilder().addParameter("playerId", LONG).addParameter("request", Request).build()
        ).build()

    val messagePackage = controllerClass.packageName.asString() + ".message"

    val messageTypes = arrayListOf<TypeSpec>()
    for (func in controllerClass.commandFunctions) {
      val messageSimpleName = func.simpleName.asString().capitalize2() + moduleName + "Request"

      val messageClassName = ClassName.bestGuess("$messagePackage.$messageSimpleName")
      val messageBuilder = TypeSpec.classBuilder(messageSimpleName).superclass(AbstractPlayerRequest)
        .addSuperclassConstructorParameter("playerId, request").addSuperinterface(interfaceType)
      val messageCtorBuilder =
        FunSpec.constructorBuilder().addParameter("playerId", LONG).addParameter("request", Request)

      val methodName = "to$messageSimpleName"
      val b = FunSpec.builder(methodName).addParameter("pr", PlayerRequest).returns(messageClassName)
      b.addStatement("val playerId = pr.playerId")
      b.addStatement("val request = pr.request")
      val paramList = StringBuilder()
      paramList.append("playerId, request")
      for (parameter in func.parameters) {
        val paramName = parameter.name!!.asString()
        val paramType = parameter.type.toTypeName2()
        if (!isRequestCommander(parameter) && paramType != Request) {
          b.addStatement("val %L = %L", paramName, readStmt(parameter))
          messageCtorBuilder.addParameter(paramName, paramType)
          messageBuilder.addProperty(
            PropertySpec.builder(
              paramName, paramType, KModifier.PUBLIC
            ).addAnnotation(JvmField::class).initializer(paramName).build()
          )
          paramList.append(',').append(' ').append(paramName)
        }
      }
      messageBuilder.primaryConstructor(messageCtorBuilder.build())
      messageTypes.add(messageBuilder.build())
      b.addStatement("return %T(%L)", messageClassName, paramList.toString())
      objectBuilder.addFunction(b.build())
      codeBlock.addStatement("%L -> %L(pr)", func.cmd, methodName)
    }
    codeBlock.addStatement("else -> %L(pr.playerId, pr.request)", baseModuleMessage.name)
    codeBlock.endControlFlow()
    convert.addCode(codeBlock.build())
    objectBuilder.addFunction(convert.build())

    val fileBuilder = FileSpec.builder(messagePackage, "Request")
    fileBuilder.addType(objectBuilder.build())
    fileBuilder.addType(baseModuleMessage)
    for (messageType in messageTypes) {
      fileBuilder.addType(messageType)
    }
    return fileBuilder.build()
  }
}
