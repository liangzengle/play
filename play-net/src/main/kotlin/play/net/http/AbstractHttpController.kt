package play.net.http

import play.util.collection.EmptyByteArray
import play.util.collection.asList
import play.util.concurrent.Future
import play.util.json.Json
import play.util.mapToObj
import play.util.reflect.isAssignableFrom
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

abstract class AbstractHttpController(actionManager: HttpActionManager) {
  init {
    val lookup = MethodHandles.lookup().`in`(javaClass)
    val baseRoute = javaClass.getAnnotation(Route::class.java)?.value ?: ""
    val actions: List<Action> = javaClass.declaredMethods
      .asSequence()
      .filter {
        it.isAnnotationPresent(Route::class.java)
          || it.isAnnotationPresent(Get::class.java)
          || it.isAnnotationPresent(Post::class.java)
      }.map { method ->
        if (!Modifier.isPublic(method.modifiers)) {
          throw IllegalStateException("$method should be public")
        }
        if (method.returnType != HttpResult::class.java) {
          throw IllegalStateException("${method}的返回值错误, 必须是${HttpResult::class.java.name}")
        }
        val subRoute = getSubRoute(method)
        val httpMethods = getHttpMethods(method)
        val routePath = RoutePath.parse(baseRoute + subRoute)
        val methodHandle = lookup.unreflect(method).bindTo(this)
        ActionImpl(routePath, httpMethods, method, methodHandle)
      }.toList()
    actionManager.register(actions)
  }

  protected fun ok(result: String): HttpResult = HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(result))

  protected fun ok(result: ByteArray): HttpResult = HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(result))

  protected fun ok(): HttpResult =
    HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(EmptyByteArray, Optional.empty()))

  protected fun error(statusCode: Int) =
    HttpResult.Strict(statusCode, HttpEntity.Strict(EmptyByteArray, Optional.empty()))

  protected fun Future<HttpResult.Strict>.toHttpResult() = HttpResult.Lazy(this)

  protected fun toJson(obj: Any) = Json.stringify(obj)

  private class ActionImpl(
    path: RoutePath,
    httpMethods: List<String>,
    private val method: Method,
    private val methodHandle: MethodHandle
  ) : Action(path, httpMethods) {
    private val parameters = method.parameters

    override fun invoke(request: AbstractHttpRequest): HttpResult {
      val parameters = getParameters(request)
      return methodHandle.invokeWithArguments(*parameters) as HttpResult
    }

    fun getParameters(httpRequest: AbstractHttpRequest): Array<Any?> {
      val result = Array<Any?>(parameters.size) { null }
      for (i in parameters.indices) {
        val p = parameters[i]
        val type = p.type
        val value = if (isAssignableFrom<BasicHttpRequest>(type)) {
          httpRequest
        } else {
          getParameter(p.name, p.parameterizedType, httpRequest, false)
        }
        result[i] = value
      }
      return result
    }

    private fun getParameter(name: String, type: Type, httpRequest: AbstractHttpRequest, isOptional: Boolean): Any {
      return if (type is ParameterizedType && type.rawType != Optional::class.java) {
        getParameter(name, type.actualTypeArguments[0], httpRequest, true)
      } else {
        val parameters = httpRequest.parameters()
        if (type == Int::class.java) {
          if (isOptional) parameters.getIntOptional(name).mapToObj { it } else parameters.getInt(name)
        } else if (type == String::class.java) {
          if (isOptional) parameters.getStringOptional(name) else parameters.getString(name)
        } else if (type == Long::class.java) {
          if (isOptional) parameters.getLongOptional(name).mapToObj { it } else parameters.getLong(name)
        } else if (type == Double::class.java) {
          if (isOptional) parameters.getDoubleOptional(name).mapToObj { it } else parameters.getDouble(name)
        } else if (type == Boolean::class.java) {
          if (isOptional) parameters.getBooleanOptional(name) else parameters.getBoolean(name)
        } else if (type == Byte::class.java) {
          if (isOptional) parameters.getByteOptional(name) else parameters.getByte(name)
        } else if (type == Short::class.java) {
          if (isOptional) parameters.getShortOptional(name) else parameters.getShort(name)
        } else if (type == OptionalInt::class.java) {
          parameters.getIntOptional(name)
        } else if (type == OptionalLong::class.java) {
          parameters.getLongOptional(name)
        } else if (type == OptionalDouble::class.java) {
          parameters.getDoubleOptional(name)
        } else if (httpRequest.hasBody()) {
          Json.toObject<Any>(httpRequest.getBodyAsString(), type)
        } else {
          throw throw UnsupportedHttpParameterTypeException(method, type)
        }
      }
    }

    override fun toString(): String {
      val b = StringBuilder()
      b.append(super.toString())
        .append(' ')
        .append(method.declaringClass.simpleName)
        .append('.')
        .append(method.name)
        .append('(')
      var first = true
      for (parameter in parameters) {
        if (!first) b.append(',')
        b.append(parameter.name)
        first = false
      }
      b.append(')')
      return b.toString()
    }
  }

  companion object {
    private val GET = HttpMethod.GET.name.asList()
    private val POST = HttpMethod.POST.name.asList()
    private val GET_POST = listOf(HttpMethod.GET.name, HttpMethod.POST.name)

    private fun getHttpMethods(method: Method): List<String> {
      val route = method.getAnnotation(Route::class.java)
      if (route != null) {
        return GET_POST
      }
      val get = method.getAnnotation(Get::class.java)
      if (get != null) {
        return GET
      }
      val post = method.getAnnotation(Post::class.java)
      if (post != null) {
        return POST
      }
      error("should not happen")
    }

    private fun getSubRoute(method: Method): String {
      val route = method.getAnnotation(Route::class.java)
      if (route != null) {
        return route.value
      }
      val get = method.getAnnotation(Get::class.java)
      if (get != null) {
        return get.value
      }
      val post = method.getAnnotation(Post::class.java)
      if (post != null) {
        return post.value
      }
      error("should not happen")
    }
  }
}
