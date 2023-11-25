package play.util.json

import com.fasterxml.jackson.databind.ObjectMapper

interface ObjectMapperConfigurator {

  fun configure(mapper: ObjectMapper)
}
