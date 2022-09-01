package play.rsocket.client.event

import org.springframework.context.ApplicationEvent

/**
 *
 * @author LiangZengle
 */
class RSocketClientInitializedEvent(uri: String) : ApplicationEvent(uri)
