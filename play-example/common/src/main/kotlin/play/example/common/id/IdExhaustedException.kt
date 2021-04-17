package play.example.common.id

class IdExhaustedException(min: Long, max: Long, current: Long) :
  RuntimeException("min=$min, max=$max, current=$current")
