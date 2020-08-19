package play.net.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import io.vavr.concurrent.Future
import io.vavr.concurrent.Promise
import org.slf4j.Logger
import play.net.http.*
import play.net.netty.toHostAndPort
import play.util.exception.isFatal
import java.util.*
import java.util.concurrent.atomic.AtomicLong

abstract class NettyHttpServerHandler(private val routeManager: RouteManager) : ChannelInboundHandlerAdapter() {

  protected abstract val filters: List<HttpRequestFilter>

  protected abstract val logger: Logger

  private val requestIdGenerator = AtomicLong()

  private fun nextId() = requestIdGenerator.incrementAndGet()

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
    if (msg !is FullHttpRequest) {
      super.channelRead(ctx, msg)
      return
    }
    try {
      val requestId = nextId()
      val hostAndPort = ctx.channel().remoteAddress().toHostAndPort()
      val request = BasicNettyHttpRequest(requestId, msg, hostAndPort)
      logAccess(request)
      if (filters.isNotEmpty() && filters.any { !it.accept(request) }) {
        writeResponse(ctx, request, HttpResult.forbidden(), "Rejected By Filter")
        return
      }

      val routeOption = routeManager.findAction(request.uri())
      if (routeOption.isEmpty) {
        writeResponse(ctx, request, HttpResult.notFount(), "Action Not Found")
        return
      }
      val route = routeOption.get()
      if (!route.acceptMethod(request.method())) {
        writeResponse(ctx, request, HttpResult.notFount(), "Method Not Supported")
        return
      }
      val parameters = parseParameters(msg)
      val pathParameters = route.path.extractPathParameters(request.uri())
      if (pathParameters.isNotEmpty()) {
        pathParameters.forEach {
          addParam(parameters, it.key, it.value)
        }
      }
      val jsonData = if (hasJsonData(msg)) msg.content().toString(Charsets.UTF_8) else ""
      msg.release()
      val req = NettyHttpRequest(request, jsonData, NettyHttpParameters(parameters))
      logRequest(req)
      handleAsync(req, route).onComplete {
        if (it.isSuccess) {
          writeResponse(ctx, request, it.get())
        } else {
          onException(ctx, request, it.cause!!)
        }
      }
    } finally {
      if (msg.refCnt() > 0) {
        msg.release()
      }
    }
  }

  private fun hasJsonData(request: FullHttpRequest): Boolean {
    return isContentType(request, HttpHeaderValues.APPLICATION_JSON)
  }

  private fun hasFormData(request: FullHttpRequest): Boolean {
    return isContentType(request, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)
  }

  private fun isContentType(request: FullHttpRequest, value: CharSequence): Boolean {
    return request.headers()[HttpHeaderNames.CONTENT_TYPE] == value.toString()
  }

  private fun parseParameters(request: FullHttpRequest): MutableMap<String, MutableList<String>> {
    val qsDecoder = QueryStringDecoder(request.uri())
    val parameters = qsDecoder.parameters()
    // post form
    if (hasFormData(request)) {
      val postDecoder = HttpPostRequestDecoder(request)
      val bodyHttpDatas = postDecoder.bodyHttpDatas
      for (i in bodyHttpDatas.indices) {
        val httpData = bodyHttpDatas[i]
        if (httpData.httpDataType == InterfaceHttpData.HttpDataType.Attribute) {
          val attribute = httpData as Attribute;
          addParam(parameters, attribute.name, attribute.value)
        }
      }
      postDecoder.destroy()
    }
    return parameters
  }

  private fun addParam(parameters: MutableMap<String, MutableList<String>>, name: String, value: String) {
    var values = parameters[name]
    if (values == null) {
      values = ArrayList(1)
      parameters[name] = values;
    }
    values.add(value)
  }

  private fun onException(ctx: ChannelHandlerContext, request: BasicNettyHttpRequest, exception: Throwable) {
    if (exception.isFatal()) {
      throw exception
    }
    val result = when (exception) {
      is HttpRequestParameterException -> HttpResult.notFount()
      else -> HttpResult.internalServerError()
    }
    writeResponse(ctx, request, result, "Exception Occurred")
  }

  private fun writeResponse(
    ctx: ChannelHandlerContext,
    request: BasicNettyHttpRequest,
    result: HttpResult.Strict,
    errorHints: String = ""
  ) {
    val resp: Any = when (val entity = result.body) {
      is HttpEntity.Strict -> {
        val response = DefaultFullHttpResponse(
          HttpVersion.HTTP_1_0,
          HttpResponseStatus.valueOf(result.status),
          Unpooled.wrappedBuffer(entity.data)
        )
        entity.contentType().forEach { response.headers().set(HttpHeaderNames.CONTENT_TYPE, it) }
        entity.contentLength().forEach { response.headers().set(HttpHeaderNames.CONTENT_LENGTH, it) }
        response
      }
      is ChunkedHttpEntity -> entity.src // make sure ChunkedWriteHandler is in the pipeline
      else -> throw UnsupportedOperationException("Unsupported HttpEntity Type: ${entity.javaClass.simpleName}")
    }
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
    logResponse(request, result, errorHints)
  }

  private fun handleAsync(request: NettyHttpRequest, route: Route): Future<HttpResult.Strict> {
    val p = Promise.make<HttpResult.Strict>()
    Future.of { route(request) }.onComplete {
      if (it.isSuccess) {
        when (val result = it.get()) {
          is HttpResult.Strict -> p.success(result)
          is HttpResult.Lazy -> p.completeWith(result.future)
        }
      } else {
        p.failure(it.cause)
      }
    }
    return p.future()
  }

  protected open fun logAccess(request: BasicNettyHttpRequest) {
    if (logger.isInfoEnabled) {
      logger.info(request.toString())
    }
  }

  protected open fun logRequest(request: NettyHttpRequest) {
    if (logger.isInfoEnabled) {
      logger.info(request.toString())
    }
  }

  protected open fun logResponse(
    request: BasicNettyHttpRequest,
    result: HttpResult,
    errorHints: String
  ) {
    if (logger.isInfoEnabled) {
      logger.info("${request.id} ${result.status} ${result.body} $errorHints")
    }
  }
}
