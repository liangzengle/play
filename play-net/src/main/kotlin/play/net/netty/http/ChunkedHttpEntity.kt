package play.net.netty.http

import io.netty.handler.stream.ChunkedInput
import io.netty.handler.stream.ChunkedNioFile
import io.netty.handler.stream.ChunkedStream
import play.net.http.HttpEntity
import java.io.File
import java.io.InputStream
import java.nio.channels.FileChannel

class ChunkedHttpEntity(val src: ChunkedInput<*>) : HttpEntity.Stream()

fun File.toHttpEntity(): ChunkedHttpEntity = ChunkedHttpEntity(ChunkedNioFile(this))
fun FileChannel.toHttpEntity(): ChunkedHttpEntity = ChunkedHttpEntity(ChunkedNioFile(this))
fun InputStream.toHttpEntity(): ChunkedHttpEntity = ChunkedHttpEntity(ChunkedStream(this))
