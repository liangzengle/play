package play.dokka

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Return
import org.jetbrains.dokka.model.doc.Text
import play.dokka.model.*
import java.util.*

/**
 *
 * @author LiangZengle
 */
object DefaultTransformer : (List<DModule>) -> ClassDescriptorList {
  override fun invoke(modules: List<DModule>): ClassDescriptorList {
    val classes = modules.asSequence().flatMap(::transform).associateByTo(TreeMap()) { it.name }
    return ClassDescriptorList(classes)
  }

  private fun transform(module: DModule): List<ClassDescriptor> {
    return module.packages.flatMap(::transform)
  }

  private fun transform(pkg: DPackage): List<ClassDescriptor> {
    return pkg.classlikes.asSequence().filterNot { it.name.isNullOrEmpty() }.map(::transform).toList()
  }

  private fun transform(clazz: DClasslike): ClassDescriptor {
    val functions = clazz.functions.map(::transform)
    val properties = clazz.properties.map(::transform)
    val qualifiedName = clazz.dri.packageName + '.' + clazz.dri.classNames
    return ClassDescriptor(qualifiedName, getDesc(clazz.documentation), functions, properties)
  }

  private fun transform(func: DFunction): FunctionDescriptor {
    val parameters = func.parameters.map(::transform)
    return FunctionDescriptor(func.name, getDesc(func.documentation), parameters, getReturnDesc(func.documentation))
  }

  private fun transform(p: DParameter): ParameterDescriptor {
    return ParameterDescriptor(p.name ?: "", typeName(p.type), getDesc(p.documentation))
  }

  private fun transform(p: DProperty): PropertyDescriptor {
    return PropertyDescriptor(p.name, getDesc(p.documentation))
  }

  fun getReturnDesc(documentation: SourceSetDependent<DocumentationNode>): String {
    for (node in documentation.values) {
      val returnNode = node.children.firstOrNull { it is Return } ?: continue
      return getDesc(returnNode.root)
    }
    return ""
  }

  fun getDesc(documentation: SourceSetDependent<DocumentationNode>): String {
    var desc = ""
    for (node in documentation.values) {
      if (node.children.isNotEmpty()) {
        desc = getDesc(node.children.first().root)
      }
    }
    return desc
  }

  tailrec fun getDesc(tag: DocTag): String {
    return if (tag is Text) {
      tag.body
    } else {
      if (tag.children.isEmpty()) "" else getDesc(tag.children.first())
    }
  }

  private fun typeName(p: Projection): String {
    return when (p) {
      is Nullable -> typeName(p.inner)
      is DefinitelyNonNullable -> typeName(p.inner)
      is PrimitiveJavaType -> p.name
      is FunctionalTypeConstructor -> "Function${p.projections.size}"
      is GenericTypeConstructor -> "${p.dri.packageName}.${p.dri.classNames}"
      is Dynamic -> "dynamic"
      is Void -> "void"
      is UnresolvedBound -> p.name
      is TypeAliased -> typeName(p.inner)
      is TypeParameter -> p.name
      is JavaObject -> "java.lang.Object"
      is Star -> "*"
      is Variance<*> -> typeName(p.inner)
    }
  }
}
