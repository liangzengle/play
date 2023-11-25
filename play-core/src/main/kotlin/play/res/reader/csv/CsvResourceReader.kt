package play.res.reader.csv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import play.res.AbstractResource
import play.res.DataType
import java.io.InputStream
import java.util.*

class CsvResourceReader {

  fun <T : AbstractResource> read(clazz: Class<T>, inputStream: InputStream) {
    inputStream.bufferedReader().use { reader ->
      val recordIterator = CSVFormat.DEFAULT.parse(reader).iterator()
      val header = readHeader(recordIterator)
      var currentRow: Array<Any>? = null
      while (recordIterator.hasNext()) {
        val row = recordIterator.next().values()
        if (isFollowerRow(row, header)) {
          appendRow(header.fieldTypes, checkNotNull(currentRow) { "" }, row)
        } else {
          currentRow = readRow(header.fieldTypes, row)
        }
      }
    }
  }

  private fun isFollowerRow(row: Array<String?>, header: CsvHeader): Boolean {
    return row[header.idIndex()].isNullOrEmpty() || row[header.nameIndex()].isNullOrEmpty()
  }

  private fun readHeader(recordIterator: Iterator<CSVRecord>): CsvHeader {
    var fieldNames: List<String>? = null
    var fieldTypes: List<DataType>? = null
    var descriptions: List<String>? = null
    while (recordIterator.hasNext()) {
      val row = recordIterator.next().values()
      val label = row[0]
      when (label) {
        CsvHeader.HEADER_FIELD_NAME, CsvHeader.HEADER_SERVER_NAME -> {
          check(fieldNames == null)
          fieldNames = Arrays.copyOfRange(row, 1, row.size).asList()
        }

        CsvHeader.HEADER_DESCRIPTION -> {
          check(descriptions == null)
          descriptions = Arrays.copyOfRange(row, 1, row.size).asList()
        }

        CsvHeader.HEADER_DATA_TYPE -> {
          check(fieldTypes == null)
          fieldTypes = Arrays.copyOfRange(row, 1, row.size).map(DataType::fromString)
          break
        }

        else -> continue
      }
    }
    if (fieldTypes.isNullOrEmpty()) {
      throw IllegalArgumentException()
    }
    if (fieldNames.isNullOrEmpty()) {
      throw IllegalArgumentException()
    }
    return CsvHeader(fieldNames, fieldTypes, descriptions ?: emptyList())
  }

  private fun readRow(dataTypes: List<DataType>, columns: Array<String>): Array<Any> {
    val parsed = arrayOfNulls<Any>(dataTypes.size)
    for (i in dataTypes.indices) {
      parsed[i] = dataTypes[i].parse(columns[i])
    }
    return parsed.requireNoNulls()
  }

  private fun appendRow(dataTypes: List<DataType>, row: Array<Any>, columns: Array<String>) {
    for (i in columns.indices) {
      val stringValue = columns[i]
      if (stringValue.isNotEmpty()) {
        val parsedValue = dataTypes[i].parse(stringValue)
        append(row, i, parsedValue)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun append(row: Array<Any>, index: Int, value: Any) {
    if (row[index] is MutableList<*>) {
      (row[index] as MutableList<Any>).add(value)
    } else {
      row[index] = arrayListOf(row[index], value)
    }
  }
}
