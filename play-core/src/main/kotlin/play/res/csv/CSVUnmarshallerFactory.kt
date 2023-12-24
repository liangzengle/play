package play.res.csv

import play.res.AbstractResource

interface CSVUnmarshallerFactory {

  fun <T : AbstractResource> create(tableId: Int): CSVUnmarshaller<T>
}
