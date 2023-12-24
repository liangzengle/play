package play.res.csv

import play.res.DataType

data class CsvHeader(
  val fieldNames: List<String>,
  val fieldTypes: List<DataType>,
  val descriptions: List<String>
) {
  companion object {
    const val HEADER_FIELD_NAME = "FieldName"
    const val HEADER_SERVER_NAME = "ServerName"

    //    const val HEADER_CLIENT_NAME = "ClientName"
    const val HEADER_DESCRIPTION = "Desc"
    const val HEADER_DATA_TYPE = "DataType"

    val FIELD_ID = "id"
    val FIELD_NAME = "name"
    val FIELD_TID = "TID"
  }

  private var nameIndex = -1
  private var idIndex = -1

  init {
    idIndex = fieldNames.indexOf(FIELD_ID)
    nameIndex = fieldNames.indexOf(FIELD_NAME)
  }

  fun idIndex(): Int {
    return idIndex
  }

  fun nameIndex(): Int {
    return nameIndex
  }

  fun getColumnIndex(columnName: String): Int {
    return fieldNames.indexOf(columnName)
  }
}
