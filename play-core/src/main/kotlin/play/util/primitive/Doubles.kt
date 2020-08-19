package play.util.primitive

import java.math.RoundingMode
import kotlin.math.ceil

/**
 * 四舍五入, 保留n位小数
 * @param scale 保留几位小数
 */
fun Double.round(scale: Int): Double = round(scale, RoundingMode.HALF_UP)

/**
 * 保留n位小数
 * @param scale 保留几位小数
 * @param roundingMode RoundingMode
 */
fun Double.round(scale: Int, roundingMode: RoundingMode): Double {
  return toBigDecimal().setScale(scale, roundingMode).toDouble()
}

fun Float.ceilToInt(): Int = ceil(this).toInt()

fun Double.ceilToInt(): Int = ceil(this).toInt()

fun Double.ceilToLong(): Long = ceil(this).toLong()
