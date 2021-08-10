package play.net.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import org.slf4j.Logger
import play.net.http.*
import play.net.netty.getHostAndPort
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.control.getCause
import play.util.exception.isFatal
import play.util.forEach
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@ChannelHandler.Sharable
abstract class NettyHttpServerHandler(protected val actionManager: HttpActionManager) : ChannelInboundHandlerAdapter() {

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
      val hostAndPort = ctx.channel().remoteAddress().getHostAndPort()
      val request = BasicNettyHttpRequest(requestId, msg, hostAndPort)
      logAccess(request)
      if (filters.isNotEmpty() && filters.any { !it.accept(request) }) {
        writeResponse(ctx, request, HttpResult.forbidden(), "Rejected By Filter")
        return
      }

      val maybeAction = findAction(request)
      if (maybeAction.isEmpty) {
        writeResponse(ctx, request, HttpResult.notFount(), "Action Not Found")
        return
      }
      val action = maybeAction.get()
      if (!action.acceptMethod(request.method())) {
        writeResponse(ctx, request, HttpResult.notFount(), "Method Not Supported")
        return
      }
      val parameters = parseParameters(msg)
      val pathParameters = action.path.extractPathParameters(request.uri())
      if (pathParameters.isNotEmpty()) {
        pathParameters.forEach {
          addParam(parameters, it.key, it.value)
        }
      }
      val body = if (hasJsonData(msg)) msg.content().toString(Charsets.UTF_8) else null
      msg.release()
      val req = NettyHttpRequest(request, body, NettyHttpParameters(parameters))
      logRequest(req)
      handleAsync(req, action).onComplete(
        { writeResponse(ctx, request, it) },
        { onException(ctx, request, it) }
      )
    } finally {
      if (msg.refCnt() > 0) {
        msg.release()
      }
    }
  }

  protected open fun findAction(request: BasicNettyHttpRequest): Optional<Action> {
    return actionManager.findAction(request.path(), request.toNetty.uri())
  }

  protected fun hasJsonData(request: FullHttpRequest): Boolean {
    return isContentType(request, HttpHeaderValues.APPLICATION_JSON)
  }

  protected fun hasFormData(request: FullHttpRequest): Boolean {
    return isContentType(request, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)
  }

  private fun isContentType(request: FullHttpRequest, value: CharSequence): Boolean {
    return request.headers()[HttpHeaderNames.CONTENT_TYPE] == value.toString()
  }

  private fun parseParameters(request: FullHttpRequest): MutableMap<String, MutableList<String>> {
    val qsDecoder = QueryStringDecoder(request.uri())
    val parameters = qsDecoder.parameters()
    // post form
    val formParams = LinkedHashMap(parameters)
    if (hasFormData(request)) {
      val postDecoder = HttpPostRequestDecoder(request)
      val bodyHttpDatas = postDecoder.bodyHttpDatas
      for (i in bodyHttpDatas.indices) {
        val httpData = bodyHttpDatas[i]
        if (httpData.httpDataType == InterfaceHttpData.HttpDataType.Attribute) {
          val attribute = httpData as Attribute
          addParam(formParams, attribute.name, attribute.value)
        }
      }
      postDecoder.destroy()
    }
    return formParams
  }

  private fun addParam(parameters: MutableMap<String, MutableList<String>>, name: String, value: String) {
    var values = parameters[name]
    if (values == null) {
      values = ArrayList(1)
      parameters[name] = values
    }
    values.add(value)
  }

  protected fun onException(ctx: ChannelHandlerContext, request: BasicNettyHttpRequest, exception: Throwable) {
    if (exception.isFatal()) {
      throw exception
    }
    val result = when (exception) {
      is HttpRequestParameterException -> HttpResult.notFount()
      else -> HttpResult.internalServerError()
    }
    writeResponse(ctx, request, result, exception.javaClass.name + ": " + exception.message)
    if (exception !is HttpRequestParameterException) {
      logger.error(exception.message, exception)
    }
  }

  protected fun writeResponse(
    ctx: ChannelHandlerContext,
    request: BasicNettyHttpRequest,
    result: HttpResult.Strict,
    errorHints: String = ""
  ) {
    val resp: Any = when (val entity = result.body) {
      is HttpEntity.Strict -> {
        val response = DefaultFullHttpResponse(
          request.toNetty.protocolVersion(),
          HttpResponseStatus.valueOf(result.status),
          Unpooled.wrappedBuffer(entity.data)
        )
        response.headers().set(HttpHeaderNames.CONTENT_ENCODING, "UTF-8")
        entity.contentType().forEach { response.headers().set(HttpHeaderNames.CONTENT_TYPE, it) }
        entity.contentLength().forEach { response.headers().set(HttpHeaderNames.CONTENT_LENGTH, it) }
        response
      }
      is ChunkedHttpEntity -> entity.src // make sure ChunkedWriteHandler is in the pipeline
      else -> throw UnsupportedOperationException("Unsupported HttpEntity Type: ${entity.javaClass.simpleName}")
    }
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE) // we do not support keep alive
    logResponse(request, result, errorHints)
  }

  private fun handleAsync(request: NettyHttpRequest, action: Action): Future<HttpResult.Strict> {
    val p = Promise.make<HttpResult.Strict>()
    Future { action(request) }.onComplete {
      if (it.isSuccess) {
        when (val result = it.getOrThrow()) {
          is HttpResult.Strict -> p.success(result)
          is HttpResult.Lazy -> p.completeWith(result.future)
        }
      } else {
        p.failure(it.getCause())
      }
    }
    return p.future
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
