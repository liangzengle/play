package play.util.el

import kotlin.math.*


@Suppress("unused", "PropertyName")
interface Math {
  /** The `Double` value that is closer than any other to `e`, the base of
   * the natural logarithms.
   *
   * @group math-const
   */
  val E get() = java.lang.Math.E

  /** The `Double` value that is closer than any other to `pi`, the ratio of
   * the circumference of a circle to its diameter.
   *
   * @group math-const
   */
  val Pi get() = java.lang.Math.PI

  /** Returns a `Double` value with a positive sign, greater than or equal
   * to `0.0` and less than `1.0`.
   *
   * @group randomisation
   */
  fun random(): Double = java.lang.Math.random()

  /** @group trig */
  fun sin(x: Double): Double = kotlin.math.sin(x)

  /** @group trig */
  fun cos(x: Double): Double = kotlin.math.cos(x)

  /** @group trig */
  fun tan(x: Double): Double = kotlin.math.tan(x)

  /** @group trig */
  fun asin(x: Double): Double = kotlin.math.asin(x)

  /** @group trig */
  fun acos(x: Double): Double = kotlin.math.acos(x)

  /** @group trig */
  fun atan(x: Double): Double = kotlin.math.atan(x)

  /** Converts an angle measured in degrees to an approximately equivalent
   * angle measured in radians.
   *
   * @param  x an angle, in degrees
   * @return the measurement of the angle `x` in radians.
   * @group angle-conversion
   */
  fun toRadians(x: Double): Double = java.lang.Math.toRadians(x)

  /** Converts an angle measured in radians to an approximately equivalent
   * angle measured in degrees.
   *
   * @param  x angle, in radians
   * @return the measurement of the angle `x` in degrees.
   * @group angle-conversion
   */
  fun toDegrees(x: Double): Double = java.lang.Math.toDegrees(x)

  /** Converts rectangular coordinates `(x, y)` to polar `(r, theta)`.
   *
   * @param  x the ordinate coordinate
   * @param  y the abscissa coordinate
   * @return the ''theta'' component of the point `(r, theta)` in polar
   *         coordinates that corresponds to the point `(x, y)` in
   *         Cartesian coordinates.
   * @group polar-coords
   */
  fun atan2(y: Double, x: Double): Double = kotlin.math.atan2(y, x)

  /** Returns the square root of the sum of the squares of both given `Double`
   * values without intermediate underflow or overflow.
   *
   * The ''r'' component of the point `(r, theta)` in polar
   * coordinates that corresponds to the point `(x, y)` in
   * Cartesian coordinates.
   *
   * @group polar-coords
   */
  fun hypot(x: Double, y: Double): Double = kotlin.math.hypot(x, y)

  // -----------------------------------------------------------------------
  // rounding functions
  // -----------------------------------------------------------------------

  /** @group rounding */
  fun ceil(x: Double): Double = kotlin.math.ceil(x)

  /** @group rounding */
  fun floor(x: Double): Double = kotlin.math.floor(x)

  /** Returns the `Double` value that is closest in value to the
   * argument and is equal to a mathematical integer.
   *
   * @param  x a `Double` value
   * @return the closest floating-point value to a that is equal to a
   *         mathematical integer.
   * @group rounding
   */
  fun rint(x: Double): Double = kotlin.math.round(x)

  /** Returns the closest `Int` to the argument.
   *
   * @param  x a floating-point value to be rounded to a `Int`.
   * @return the value of the argument rounded to the nearest `Int` value.
   * @group rounding
   */
  fun round(x: Float): Int = x.roundToInt()

  /** Returns the closest `Long` to the argument.
   *
   * @param  x a floating-point value to be rounded to a `Long`.
   * @return the value of the argument rounded to the nearest`long` value.
   * @group rounding
   */
  fun round(x: Double): Long = x.roundToLong()

  /** @group abs */
  fun abs(x: Int): Int = kotlin.math.abs(x)

  /** @group abs */
  fun abs(x: Long): Long = kotlin.math.abs(x)

  /** @group abs */
  fun abs(x: Float): Float = kotlin.math.abs(x)

  /** @group abs */
  fun abs(x: Double): Double = kotlin.math.abs(x)

  /** @group minmax */
  fun max(x: Int, y: Int): Int = kotlin.math.max(x, y)

  /** @group minmax */
  fun max(x: Long, y: Long): Long = kotlin.math.max(x, y)

  /** @group minmax */
  fun max(x: Float, y: Float): Float = kotlin.math.max(x, y)

  /** @group minmax */
  fun max(x: Double, y: Double): Double = kotlin.math.max(x, y)

  /** @group minmax */
  fun min(x: Int, y: Int): Int = kotlin.math.min(x, y)

  /** @group minmax */
  fun min(x: Long, y: Long): Long = kotlin.math.min(x, y)

  /** @group minmax */
  fun min(x: Float, y: Float): Float = kotlin.math.min(x, y)

  /** @group minmax */
  fun min(x: Double, y: Double): Double = kotlin.math.min(x, y)

  fun ceilDiv(x: Int, y: Int): Int = java.lang.Math.ceilDivExact(x, y)
  fun floorDiv(x: Int, y: Int): Int = java.lang.Math.floorDivExact(x, y)

  fun ceilDiv(x: Long, y: Long): Long = java.lang.Math.ceilDivExact(x, y)
  fun floorDiv(x: Long, y: Long): Long = java.lang.Math.floorDivExact(x, y)

  fun ceilMod(x: Long, y: Long): Long = java.lang.Math.ceilMod(x, y)
  fun floorMod(x: Long, y: Long): Long = java.lang.Math.floorMod(x, y)

  fun clamp(x: Long, min: Int, max: Int): Int = java.lang.Math.clamp(x, min, max)
  fun clamp(x: Long, min: Long, max: Long): Long = java.lang.Math.clamp(x, min, max)
  fun clamp(x: Float, min: Float, max: Float): Float = java.lang.Math.clamp(x, min, max)
  fun clamp(x: Double, min: Double, max: Double): Double = java.lang.Math.clamp(x, min, max)

  /** @group signum
   * @note Forwards to [[java.lang.Integer]]
   */
  fun signum(x: Int): Int = Integer.signum(x)

  /** @group signum
   * @note Forwards to [[java.lang.Long]]
   */
  fun signum(x: Long): Int = java.lang.Long.signum(x)

  /** @group signum */
  fun signum(x: Float): Float = sign(x)

  /** @group signum */
  fun signum(x: Double): Double = sign(x)

  // -----------------------------------------------------------------------
  // root functions
  // -----------------------------------------------------------------------

  /** Returns the square root of a `Double` value.
   *
   * @param  x the number to take the square root of
   * @return the value √x
   * @group root-extraction
   */
  fun sqrt(x: Double): Double = kotlin.math.sqrt(x)

  /** Returns the cube root of the given `Double` value.
   *
   * @param  x the number to take the cube root of
   * @return the value ∛x
   * @group root-extraction
   */
  fun cbrt(x: Double): Double = java.lang.Math.cbrt(x)

  // -----------------------------------------------------------------------
  // exponential functions
  // -----------------------------------------------------------------------

  /** Returns the value of the first argument raised to the power of the
   * second argument.
   *
   * @param x the base.
   * @param y the exponent.
   * @return the value `x^y^`.
   * @group explog
   */
  fun pow(x: Double, y: Double): Double = x.pow(y)

  /** Returns Euler's number `e` raised to the power of a `Double` value.
   *
   * @param  x the exponent to raise `e` to.
   * @return the value `e^a^`, where `e` is the base of the natural
   *         logarithms.
   * @group explog
   */
  fun exp(x: Double): Double = kotlin.math.exp(x)

  /** Returns `exp(x) - 1`.
   *
   * @group explog
   */
  fun expm1(x: Double): Double = kotlin.math.expm1(x)

  // -----------------------------------------------------------------------
  // logarithmic functions
  // -----------------------------------------------------------------------

  /** Returns the natural logarithm of a `Double` value.
   *
   * @param  x the number to take the natural logarithm of
   * @return the value `logₑ(x)` where `e` is Eulers number
   * @group explog
   */
  fun log(x: Double): Double = ln(x)

  /** Returns the natural logarithm of the sum of the given `Double` value and 1.
   *
   * @group explog
   */
  fun log1p(x: Double): Double = ln1p(x)

  /** Returns the base 10 logarithm of the given `Double` value.
   *
   * @group explog
   */
  fun log10(x: Double): Double = kotlin.math.log10(x)

  // -----------------------------------------------------------------------
  // trigonometric functions
  // -----------------------------------------------------------------------

  /** Returns the hyperbolic sine of the given `Double` value.
   *
   * @group hyperbolic
   */
  fun sinh(x: Double): Double = kotlin.math.sinh(x)

  /** Returns the hyperbolic cosine of the given `Double` value.
   *
   * @group hyperbolic
   */
  fun cosh(x: Double): Double = kotlin.math.cosh(x)

  /** Returns the hyperbolic tangent of the given `Double` value.
   *
   * @group hyperbolic
   */
  fun tanh(x: Double): Double = kotlin.math.tanh(x)

  // -----------------------------------------------------------------------
  // miscellaneous functions
  // -----------------------------------------------------------------------

  /** Returns the size of an ulp of the given `Double` value.
   *
   * @group ulp
   */
  fun ulp(x: Double): Double = java.lang.Math.ulp(x)

  /** Returns the size of an ulp of the given `Float` value.
   *
   * @group ulp
   */
  fun ulp(x: Float): Float = java.lang.Math.ulp(x)

  /** @group rounding */
  fun IEEEremainder(x: Double, y: Double): Double = x.IEEErem(y)

  companion object : Math
}
