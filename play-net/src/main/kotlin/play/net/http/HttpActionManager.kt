package play.net.http

import play.util.collection.asList
import play.util.reflect.isAssignable
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

abstract class HttpActionManager {

  private var plainActions = emptyMap<String, Action>()

  private var variableActions = emptyList<Action>()

  fun findAction(uri: String): Optional<Action> {
    val action = plainActions[uri]
    if (action != null) {
      return Optional.of(action)
    }
    for (a in variableActions) {
      if (a.path.matches(uri)) {
        return Optional.of(a)
      }
    }
    return Optional.empty()
  }

  @Synchronized
  fun register(controller: AbstractHttpController) {
    val maybeRoute = controller.javaClass.getAnnotation(Route::class.java)
    if (maybeRoute != null && maybeRoute.methods.isNotEmpty()) {
      throw UnsupportedOperationException("Route.methods should be empty: ${controller.javaClass.name}")
    }
    val plainActions = this.plainActions.toMutableMap()
    val variableActions = this.variableActions.toMutableList()
    val baseRoute = maybeRoute?.value ?: ""
    controller
      .javaClass
      .declaredMethods
      .asSequence()
      .filter {
        it.isAnnotationPresent(Route::class.java)
          || it.isAnnotationPresent(Get::class.java)
          || it.isAnnotationPresent(Post::class.java)
      }
      .forEach { method ->
        if (!Modifier.isPublic(method.modifiers)) {
          throw IllegalStateException("$method should be public")
        }
        if (method.returnType != HttpResult::class.java) {
          throw IllegalStateException("${method}的返回值错误, 必须是${HttpResult::class.java.name}")
        }
        val subRoute = getSubRoute(method)
        val httpMethods = getHttpMethods(method)
        val routePath = RoutePath.parse(baseRoute + subRoute)
        MethodHandles.lookup().`in`(controller.javaClass)
        val methodHandle = createMethodHandle(method)
        methodHandle.bindTo(controller)
        val action = ActionImpl(routePath, httpMethods, method, methodHandle)
        if (routePath.isPlain()) {
          plainActions.putIfAbsent(routePath.root, action)
        } else {
          variableActions.add(action)
        }
      }
    variableActions.sortBy { it.path.root }
    this.plainActions = plainActions
    this.variableActions = variableActions
  }

  private fun createMethodHandle(method: Method): MethodHandle {
    val lookup = MethodHandles.publicLookup()
    val methodType = MethodType.methodType(method.returnType, method.parameterTypes)
    return lookup.findSpecial(method.declaringClass, method.name, methodType, method.declaringClass)
  }

  private fun getHttpMethods(method: Method): List<String> {
    val route = method.getAnnotation(Route::class.java)
    if (route != null) {
      val hasGet = route.methods.contains(HttpMethod.GET)
      val hasPost = route.methods.contains(HttpMethod.POST)
      return if (hasGet && !hasPost) GET else if (hasPost && !hasGet) POST else GET_POST
    }
    val get = method.getAnnotation(Get::class.java)
    if (get != null) {
      return GET
    }
    val post = method.getAnnotation(Post::class.java)
    if (post != null) {
      return POST
    }
    throw Error()
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
    throw Error()
  }

  private class ActionImpl(
    path: RoutePath,
    methods: List<String>,
    private val method: Method,
    private val methodHandle: MethodHandle
  ) : Action(path, methods) {

    override fun invoke(request: AbstractHttpRequest): HttpResult {
      val parameters = getParameters(request)
      return methodHandle.invokeExact(parameters) as HttpResult
    }

    fun getParameters(httpRequest: AbstractHttpRequest): Array<Any> {
      val result = Array<Any>(method.parameterCount) { "" }
      val parameters = method.parameters
      for (i in method.parameters.indices) {
        val p = parameters[i]
        val type = p.type
        val value = if (isAssignable<BasicHttpRequest>(type)) {
          httpRequest
        } else {
          getParameter(p.name, p.parameterizedType, httpRequest, false)
        }
        result[i] = value
      }
      return result
    }

    private fun getParameter(name: String, type: Type, httpRequest: AbstractHttpRequest, isOptional: Boolean): Any {
      return if (type is ParameterizedType) {
        if (type.rawType != Optional::class.java) {
          throw UnsupportedHttpParameterTypeException(method, type)
        }
        getParameter(name, type.actualTypeArguments[0], httpRequest, true)
      } else {
        val parameters = httpRequest.parameters()
        if (type == Int::class.java) {
          if (isOptional) parameters.getIntOptional(name) else parameters.getInt(name)
        } else if (type == String::class.java) {
          if (isOptional) parameters.getStringOptional(name) else parameters.getString(name)
        } else if (type == Boolean::class.java) {
          if (isOptional) parameters.getBooleanOptional(name) else parameters.getBoolean(name)
        } else if (type == Byte::class.java) {
          if (isOptional) parameters.getByteOptional(name) else parameters.getByte(name)
        } else if (type == Short::class.java) {
          if (isOptional) parameters.getShortOptional(name) else parameters.getShort(name)
        } else if (type == Long::class.java) {
          if (isOptional) parameters.getLongOptional(name) else parameters.getLong(name)
        } else if (type == Double::class.java) {
          if (isOptional) parameters.getDoubleOptional(name) else parameters.getDouble(name)
        } else {
          throw throw UnsupportedHttpParameterTypeException(method, type)
        }
      }
    }
  }

  companion object {
    private val GET = HttpMethod.GET.name.asList()
    private val POST = HttpMethod.POST.name.asList()
    private val GET_POST = listOf(HttpMethod.GET.name, HttpMethod.POST.name)
  }
}
