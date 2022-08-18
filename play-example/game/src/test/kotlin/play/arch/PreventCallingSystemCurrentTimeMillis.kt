package play.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import org.junit.jupiter.api.Test

/**
 *
 *
 * @author LiangZengle
 */
class PreventCallingSystemCurrentTimeMillis {
  @Test
  fun preventCallingSystem_currentTimeMillis() {
    val importedClasses = ClassFileImporter().importPackages("play")
    val rule = ArchRuleDefinition.noClasses().should()
      .callMethod(System::class.java, "currentTimeMillis")
      .because("Should use Time.currentMillis instead")
    rule.check(importedClasses)
  }
}
