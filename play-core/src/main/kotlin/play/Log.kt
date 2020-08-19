package play

import org.slf4j.LoggerFactory

/**
 * 不记录调用点的日志
 * @author LiangZengle
 */
object Log : Logger(LoggerFactory.getLogger("application"))
