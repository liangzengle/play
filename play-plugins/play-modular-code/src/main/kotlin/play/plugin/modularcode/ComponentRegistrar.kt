package play.plugin.modularcode

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class ModularCodeComponentRegistrar : CompilerPluginRegistrar() {

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (configuration.get(ModularCodeConfigurationKeys.KEY_ENABLED) == false) {
      return
    }
    ClassBuilderInterceptorExtension.registerExtension(
      ModularCodeClassGeneratorInterceptor(configuration[ModularCodeConfigurationKeys.KEY_ANNOTATION]!!)
    )
  }
}

class ModularCodeClassGeneratorInterceptor(annotation: String) : ClassBuilderInterceptorExtension {
  private val annotationFqName = FqName(annotation)

  override fun interceptClassBuilderFactory(
    interceptedFactory: ClassBuilderFactory,
    bindingContext: BindingContext,
    diagnostics: DiagnosticSink
  ): ClassBuilderFactory {
    return object : ClassBuilderFactory {
      override fun getClassBuilderMode(): ClassBuilderMode {
        return interceptedFactory.classBuilderMode
      }

      override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
        return if (origin.descriptor?.annotations?.hasAnnotation(annotationFqName) == true) {
          ModularCodeClassBuilder(interceptedFactory.newClassBuilder(origin))
        } else {
          interceptedFactory.newClassBuilder(origin)
        }
      }

      override fun asText(builder: ClassBuilder): String {
        return when (builder) {
          is ModularCodeClassBuilder -> interceptedFactory.asText(builder.classBuilder)
          else -> interceptedFactory.asText(builder)
        }
      }

      override fun asBytes(builder: ClassBuilder): ByteArray {
        return when (builder) {
          is ModularCodeClassBuilder -> interceptedFactory.asBytes(builder.classBuilder)
          else -> interceptedFactory.asBytes(builder)
        }
      }

      override fun close() {
        return interceptedFactory.close()
      }
    }
  }
}

class ModularCodeClassBuilder(val classBuilder: ClassBuilder) :
  DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder {
    return classBuilder
  }

  override fun newField(
    origin: JvmDeclarationOrigin,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    value: Any?
  ): FieldVisitor {
    return if (value is Int && Modifier.isFinal(access) && Modifier.isStatic(access) && Modifier.isPublic(access)) {
      throw IllegalStateException("${origin.descriptor} can not be final")
    } else {
      super.newField(origin, access, name, desc, signature, value)
    }
  }

  override fun newMethod(
    origin: JvmDeclarationOrigin,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    val original = super.newMethod(origin, access, name, desc, signature, exceptions)
    return if ("<clinit>" != name) {
      original
    } else {
      val classDescriptor = origin.descriptor as ClassDescriptor
      val owner = classDescriptor.fqNameSafe.toString().replace('.', '/')
      object : MethodVisitor(Opcodes.ASM7, original) {

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
}
