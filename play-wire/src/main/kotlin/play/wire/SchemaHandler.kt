package play.wire

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.WireCompiler
import play.wire.com.squareup.wire.koltin.KotlinGenerator
import com.squareup.wire.schema.*
import okio.Path
import java.io.IOException

/**
 *
 * @author LiangZengle
 */
class PlaySchemaHandlerFactory : SchemaHandler.Factory {
  override fun create(): SchemaHandler {
    return PlaySchemaHandler()
  }
}

class PlaySchemaHandler : SchemaHandler() {

  private lateinit var kotlinGenerator: KotlinGenerator

  override fun handle(schema: Schema, context: Context) {
    kotlinGenerator = KotlinGenerator(schema)
    context.fileSystem.createDirectories(context.outDirectory)
    super.handle(schema, context)
  }

  override fun handle(extend: Extend, field: Field, context: Context): Path? {
    val typeSpec = kotlinGenerator.generateOptionType(extend, field) ?: return null
    val name = kotlinGenerator.generatedTypeName(extend.member(field))
    return write(name, typeSpec, field.qualifiedName, field.location, context)
  }

  override fun handle(service: Service, context: Context): List<Path> {
    throw UnsupportedOperationException()
  }

  override fun handle(type: Type, context: Context): Path? {
    if (KotlinGenerator.builtInType(type.type)) return null

    val typeSpec = kotlinGenerator.generateType(type)
    val className = kotlinGenerator.generatedTypeName(type)
    return write(className, typeSpec, type.type, type.location, context)
  }

  private fun write(
    name: ClassName,
    typeSpec: TypeSpec,
    source: Any,
    location: Location,
    context: Context,
  ): Path {
    val modulePath = context.outDirectory
    val kotlinFile = FileSpec.builder(name.packageName, name.simpleName)
      .addFileComment(WireCompiler.CODE_GENERATED_BY_WIRE)
      .addFileComment("\nSource: %L in %L", source, location.withPathOnly())
      .addType(typeSpec)
      .build()
    val filePath = modulePath /
      kotlinFile.packageName.replace(".", "/") /
      "${kotlinFile.name}.kt"

    context.logger.artifactHandled(
      modulePath, "${kotlinFile.packageName}.${(kotlinFile.members.first() as TypeSpec).name}",
      "Kotlin"
    )
    try {
      context.fileSystem.createDirectories(filePath.parent!!)
      context.fileSystem.write(filePath) {
        writeUtf8(kotlinFile.toString())
      }
    } catch (e: IOException) {
      throw IOException("Error emitting ${kotlinFile.packageName}.$source to ${context.outDirectory}", e)
    }
    return filePath
  }
}
