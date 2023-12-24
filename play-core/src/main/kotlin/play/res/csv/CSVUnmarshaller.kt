package play.res.csv

import play.res.AbstractResource
import play.res.ResourceUnmarshaller

interface CSVUnmarshaller<O : AbstractResource> : ResourceUnmarshaller<CsvRow, O>
