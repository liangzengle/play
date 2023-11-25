package play.res

enum class DataType {
  STRING {
    override fun parse(stringValue: String): Any {
      return stringValue
    }

    override fun defaultValue(): Any {
      return ""
    }
  },
  INT {
    override fun parse(stringValue: String): Any {
      return if (stringValue.isEmpty()) 0 else stringValue.toInt()
    }

    override fun defaultValue(): Any {
      return 0
    }
  },
  LONG {
    override fun parse(stringValue: String): Any {
      return if (stringValue.isEmpty()) 0L else stringValue.toLong()
    }

    override fun defaultValue(): Any {
      return 0L
    }
  },
  BOOL {
    override fun parse(stringValue: String): Any {
      return when (stringValue.lowercase()) {
        "1", "true", "enable", "enabled", "on" -> true
        else -> false
      }
    }

    override fun defaultValue(): Any {
      return false
    }
  };

  abstract fun parse(stringValue: String): Any

  abstract fun defaultValue(): Any

  companion object {
    @JvmStatic
    fun fromString(dataType: String): DataType {
      return when (dataType.lowercase()) {
        "string" -> STRING
        "int" -> INT
        "long" -> LONG
        "bool", "boolean" -> BOOL
        else -> throw IllegalArgumentException("Unsupported data type `$dataType`")
      }
    }
  }
}
