package play.example.game.container.gs.entity

import play.codegen.DisableCodegen
import play.entity.ImmutableEntity
import play.entity.IntIdEntity

/**
 *
 * @author LiangZengle
 */
@DisableCodegen
@ImmutableEntity
class GameServerEntity(id: Int) : IntIdEntity(id)
