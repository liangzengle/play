package play.example.module.mail.domain

sealed class ReceiverQualification

object EmptyQualification : ReceiverQualification()

data class PlayerSetQualification(val playerIds: Set<Long>) : ReceiverQualification()

