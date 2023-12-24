package play.res

interface ResourceUnmarshaller<I, O: AbstractResource> {

  fun unmarshal(input: I): O
}
