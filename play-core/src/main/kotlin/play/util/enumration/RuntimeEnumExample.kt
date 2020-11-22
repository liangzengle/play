package play.util.enumration

class Planet(id: Int, name: String, desc: String) : RuntimeEnum(id, name, desc) {

  init {
    create(this)
  }

  companion object : RuntimeEnumFactoryOps<Planet> by Factory(Planet::class.java)
}

object PlanetEnumUsage {
  val Earth = Planet(1, "Earth", "地球")
  val Earth2 = Planet(2, "Earth2", "地球")

  @JvmStatic
  fun main(args: Array<String>) {
    println(Planet.values())
    println(Planet.getOrThrow(1) === Earth)
  }
}
