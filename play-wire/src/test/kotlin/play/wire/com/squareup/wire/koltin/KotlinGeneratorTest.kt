package play.wire.com.squareup.wire.koltin

import org.junit.jupiter.api.Test
import com.squareup.wire.buildSchema
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertTrue

/**
 *
 * @author LiangZengle
 */
internal class KotlinGeneratorTest {

  @Test
  fun test() {
    val schema = buildSchema {
      add(
        "play.proto".toPath(),
        """
          package play;
          import "google/protobuf/descriptor.proto";
          
          message MessageOptions {
            repeated string implements = 1;
          }
          
          extend google.protobuf.MessageOptions {
            optional MessageOptions play_message_option = 5000;
          }
          
          message FieldOptions {
            optional bool override = 1;
          }

          extend google.protobuf.FieldOptions {
            optional FieldOptions play_field_option = 5000;
          }
        """.trimIndent()
      )
      add(
        "message.proto".toPath(),
        """
        |import "play.proto";
        |message Person {
        | option (play.play_message_option).implements = "MyInterface";
        |	required string name = 1 [(play.play_field_option).override = true];
        |	required int32 id = 2;
        |	optional string email = 3;
        |	enum PhoneType {
        |		HOME = 0;
        |		WORK = 1;
        |		MOBILE = 2;
        |	}
        |	message PhoneNumber {
        |		required string number = 1;
        |		optional PhoneType type = 2 [default = HOME];
        |	}
        |	repeated PhoneNumber phone = 4;
        |}""".trimMargin()
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Person")
    assertTrue(code.contains(", MyInterface"))
    assertTrue(code.contains("public override val name: String"))
  }
}
