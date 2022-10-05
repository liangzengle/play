package play.example.game.app.module.server.condition

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 *
 * @author LiangZengle
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class ServerCondition
