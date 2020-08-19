package play.config

interface ConfigValidator {

  fun validate(setManager: ConfigSetManager, errors: MutableList<String>)
}
