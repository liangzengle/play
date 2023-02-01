package org.eclipse.collections.impl.list.immutable.primitive

import org.eclipse.collections.api.list.primitive.*

internal fun newListWith(vararg elements: Boolean): ImmutableBooleanList {
  return ImmutableBooleanArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Byte): ImmutableByteList {
  return ImmutableByteArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Char): ImmutableCharList {
  return ImmutableCharArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Short): ImmutableShortList {
  return ImmutableShortArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Int): ImmutableIntList {
  return ImmutableIntArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Long): ImmutableLongList {
  return ImmutableLongArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Float): ImmutableFloatList {
  return ImmutableFloatArrayList.newListWith(*elements)
}

internal fun newListWith(vararg elements: Double): ImmutableDoubleList {
  return ImmutableDoubleArrayList.newListWith(*elements)
}
