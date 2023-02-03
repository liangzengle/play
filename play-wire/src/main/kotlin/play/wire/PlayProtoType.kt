package play.wire

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.ProtoType
import org.eclipse.collections.api.factory.primitive.*
import play.wire.com.squareup.wire.koltin.keyType
import play.wire.com.squareup.wire.koltin.valueType

/**
 *
 * @author LiangZengle
 */
internal object PlayProtoType {

  private const val IntListValue = "play.IntListValue"
  private const val LongListValue = "play.LongListValue"
  private const val FloatListValue = "play.FloatListValue"
  private const val DoubleListValue = "play.DoubleListValue"
  private const val IntIntMapValue = "play.IntIntMapValue"
  private const val IntLongMapValue = "play.IntLongMapValue"
  private const val LongIntMapValue = "play.LongIntMapValue"
  private const val LongLongMapValue = "play.LongLongMapValue"

  private val asJava = MemberName("org.eclipse.collectionx", "asJava")

  @JvmField
  val INT_LIST_VALUE = ProtoType.get(IntListValue)

  @JvmField
  val LONG_LIST_VALUE = ProtoType.get(LongListValue)

  @JvmField
  val FLOAT_LIST_VALUE = ProtoType.get(FloatListValue)

  @JvmField
  val DOUBLE_LIST_VALUE = ProtoType.get(DoubleListValue)

  @JvmField
  val INT_INT_MAP_VALUE = ProtoType.get(IntIntMapValue)

  @JvmField
  val INT_LONG_MAP_VALUE = ProtoType.get(IntLongMapValue)

  @JvmField
  val LONG_INT_MAP_VALUE = ProtoType.get(LongIntMapValue)

  @JvmField
  val LONG_LONG_MAP_VALUE = ProtoType.get(LongLongMapValue)

  fun getIdentity(protoType: ProtoType): CodeBlock? {
    return when (protoType.toString()) {
      IntListValue -> CodeBlock.of("%T.EMPTY", play.IntListValue::class)
      LongListValue -> CodeBlock.of("%T.EMPTY", play.LongListValue::class)
      FloatListValue -> CodeBlock.of("%T.EMPTY", play.FloatListValue::class)
      DoubleListValue -> CodeBlock.of("%T.EMPTY", play.DoubleListValue::class)
      IntIntMapValue -> CodeBlock.of("%T.EMPTY", play.IntIntMapValue::class)
      IntLongMapValue -> CodeBlock.of("%T.EMPTY", play.IntLongMapValue::class)
      LongIntMapValue -> CodeBlock.of("%T.EMPTY", play.LongIntMapValue::class)
      LongLongMapValue -> CodeBlock.of("%T.EMPTY", play.LongLongMapValue::class)
      else -> null
    }
  }

  fun newList(field: Field): CodeBlock? {
    return when (field.type) {
      ProtoType.INT32 -> CodeBlock.of("%T.mutable.empty().%M()", IntLists::class, asJava)
      ProtoType.INT64 -> CodeBlock.of("%T.mutable.empty().%M()", LongLists::class, asJava)
      ProtoType.FLOAT -> CodeBlock.of("%T.mutable.empty().%M()", FloatLists::class, asJava)
      ProtoType.DOUBLE -> CodeBlock.of("%T.mutable.empty().%M()", DoubleLists::class, asJava)
      ProtoType.BOOL -> CodeBlock.of("%T.mutable.empty().%M()", BooleanLists::class, asJava)
      else -> null
    }
  }

  fun listFieldDeclaration(field: Field, allocatedName: String): CodeBlock? {
    return when (field.type) {
      ProtoType.INT32 -> CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", IntLists::class, asJava)
      ProtoType.INT64 -> CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", LongLists::class, asJava)
      ProtoType.FLOAT -> CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", FloatLists::class, asJava)
      ProtoType.DOUBLE -> CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", DoubleLists::class, asJava)
      ProtoType.BOOL -> CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", BooleanLists::class, asJava)
      else -> null
    }
  }

  fun newMap(field: Field): CodeBlock? {
    if (field.keyType == ProtoType.INT32 && field.valueType == ProtoType.INT32) {
      return CodeBlock.of("%T.mutable.empty().%M()", IntIntMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT32 && field.valueType == ProtoType.INT64) {
      return CodeBlock.of("%T.mutable.empty().%M()", IntLongMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT64 && field.valueType == ProtoType.INT32) {
      return CodeBlock.of("%T.mutable.empty().%M()", LongIntMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT64 && field.valueType == ProtoType.INT64) {
      return CodeBlock.of("%T.mutable.empty().%M()", LongLongMaps::class, asJava)
    }
    return null
  }

  fun mapFieldDeclaration(field: Field, allocatedName: String): CodeBlock? {
    if (field.keyType == ProtoType.INT32 && field.valueType == ProtoType.INT32) {
      return CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", IntIntMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT32 && field.valueType == ProtoType.INT64) {
      return CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", IntLongMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT64 && field.valueType == ProtoType.INT32) {
      return CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", LongIntMaps::class, asJava)
    }
    if (field.keyType == ProtoType.INT64 && field.valueType == ProtoType.INT64) {
      return CodeBlock.of("val $allocatedName = %T.mutable.empty().%M()", LongLongMaps::class, asJava)
    }
    return null
  }
}
