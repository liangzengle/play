package play.plugin.modularcode

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.*
import java.lang.reflect.Modifier

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class ModularCodeComponentRegistrar : CompilerPluginRegistrar() {

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (configuration.get(ModularCodeConfigurationKeys.KEY_ENABLED) == false) {
      return
    }
    ClassGeneratorExtension.registerExtension(
      ModularCodeClassGeneratorInterceptor(configuration[ModularCodeConfigurationKeys.KEY_ANNOTATION]!!)
    )
  }
}

class ModularCodeClassGeneratorInterceptor(annotation: String) : ClassGeneratorExtension {
  private val annotationFqName = FqName(annotation)

  override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator {
    if (declaration == null || declaration.annotations.none { it.annotationClass.fqNameForIrSerialization == annotationFqName }) {
      return generator
    }
    return object : ClassGenerator {
      override fun defineClass(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<out String>
      ) {
        generator.defineClass(version, access, name, signature, superName, interfaces)
      }

      override fun done(generateSmapCopyToAnnotation: Boolean) {
        generator.done(generateSmapCopyToAnnotation)
      }

      override fun newField(
        declaration: IrField?,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        value: Any?
      ): FieldVisitor {
        if (declaration == null) {
          return generator.newField(null, access, name, desc, signature, value)
        }
        return if (Modifier.isPublic(access) && declaration.isStatic && declaration.isFinal) {
          throw IllegalStateException("$signature can not be public static final")
        } else {
          generator.newField(declaration, access, name, desc, signature, value)
        }
      }

      override fun newMethod(
        declaration: IrFunction?,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
      ): MethodVisitor {
        val origin = generator.newMethod(declaration, access, name, desc, signature, exceptions)
        return if ("<clinit>" != name) {
          origin
        } else {
          checkNotNull(declaration)
          println("declaration.parentAsClass.name: ${declaration.parentAsClass.name}")
          println("declaration.parentAsClass.fqNameForIrSerialization: ${declaration.parentAsClass.fqNameForIrSerialization}")
          println("declaration.parentAsClass.fqNameWhenAvailable: ${declaration.parentAsClass.fqNameWhenAvailable}")
          println("declaration.parentAsClass.kotlinFqName: ${declaration.parentAsClass.kotlinFqName}")
          val owner = declaration.parentAsClass.fqNameForIrSerialization.toString().replace('.', '/')
          object : MethodVisitor(Opcodes.ASM7, origin) {

            override fun visitIntInsn(opcode: Int, `var`: Int) {
              super.visitIntInsn(opcode, `var`)
              when (opcode) {
                Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.LDC -> intercept()
              }
            }

            override fun visitInsn(opcode: Int) {
              super.visitInsn(opcode)
              when (opcode) {
                Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 -> intercept()
              }
            }

            private fun intercept() {
              super.visitVarInsn(Opcodes.ALOAD, 0)
              super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                owner,
                "getModuleId",
                "()S",
                false
              )
              super.visitIntInsn(Opcodes.SIPUSH, 1000)
              super.visitInsn(Opcodes.IMUL)
              super.visitInsn(Opcodes.IADD)
            }
          }
        }
      }

      override fun newRecordComponent(name: String, desc: String, signature: String?): RecordComponentVisitor {
        return generator.newRecordComponent(name, desc, signature)
      }

      override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
        return generator.visitAnnotation(desc, visible)
      }

      override fun visitEnclosingMethod(owner: String, name: String?, desc: String?) {
        return generator.visitEnclosingMethod(owner, name, desc)
      }

      override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        return generator.visitInnerClass(name, outerName, innerName, access)
      }

      override fun visitSource(name: String, debug: String?) {
        return generator.visitSource(name, debug)
      }
    }
  }
}
