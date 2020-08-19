package play.db

import java.io.Closeable

interface Repository : PersistService, QueryService, Closeable
