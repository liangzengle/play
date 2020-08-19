package play.collection.primive.immutable.list

import it.unimi.dsi.fastutil.ints.IntArrayList

fun newIntArrayList(): List<Int> = IntArrayList()

fun newIntArrayList(initialSize: Int): List<Int> = IntArrayList(initialSize)
