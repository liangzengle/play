package play

import org.slf4j.LoggerFactory
import play.util.logging.Logger

/**
 * 不记录调用点的日志
 * @author LiangZengle
 */
val Log = Logger(LoggerFactory.getLogger("application"))
