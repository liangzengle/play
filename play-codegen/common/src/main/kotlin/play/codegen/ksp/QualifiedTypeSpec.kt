package play.codegen.ksp

import com.squareup.kotlinpoet.TypeSpec

/**
 *
 * @author LiangZengle
 */
class QualifiedTypeSpec(private val typeSpec: TypeSpec, private val pkg: String) {
  val simpleName get() = typeSpec.name!!
  val qualifiedName = "$pkg.${typeSpec.name}"

  fun getPackage() = pkg

  fun getTypeSpec() = typeSpec

  override fun equals(other: Any?): Boolean {
    return other === this || other is QualifiedTypeSpec && other.pkg == this.pkg && other.simpleName == this.simpleName
  }

  override fun hashCode(): Int {
    var result = pkg.hashCode()
    result = 31 * result + simpleName.hashCode()
    return result
  }
}
