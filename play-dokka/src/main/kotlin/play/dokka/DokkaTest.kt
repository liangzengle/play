package play.dokka

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 *
 * @author LiangZengle
 */
internal object DokkaTest {
  private val objectMapper = ObjectMapper()

  @JvmStatic
  fun main(args: Array<String>) {
    val result = Dokka.generate(File("E:\\WorkSpaces\\IdeaProjects\\play\\play-example\\game"))
    println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.classes))
  }
}
