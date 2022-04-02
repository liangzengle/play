package play.codegen.ksp

import com.squareup.kotlinpoet.TypeSpec

/**
 *
 * @author LiangZengle
 */
class TypeSpecWithPackage(private val typeSpec: TypeSpec, private val pkg: String) {
  val simpleName get() = typeSpec.name!!
  val qualifiedName = "$pkg.${typeSpec.name}"

  fun getPackage() = pkg

  fun getTypeSpec() = typeSpec

  override fun equals(other: Any?): Boolean {
    return other === this || other is TypeSpecWithPackage && other.qualifiedName == this.qualifiedName
  }

  override fun hashCode(): Int {
    return qualifiedName.hashCode()
  }
}
