package play.example.game.container.gs.entity

import play.codegen.DisableCodegen
import play.entity.IntIdEntity
import play.entity.cache.ImmutableEntity

/**
 *
 * @author LiangZengle
 */
@DisableCodegen
@ImmutableEntity
class GameServerEntity(id: Int) : IntIdEntity(id)
