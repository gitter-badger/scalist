package ru.pavkin.todoist.api.core

import shapeless.{:+:, CNil}

package object dto {

  type Item = Task

  case class RawCommandError(error_code: Int, error: String)
  type TempIdMapping = Map[String, Int]
  type RawCommandStatus = String :+: RawCommandError :+: RawMultipleItemCommandStatus :+: CNil
  type RawItemStatus = String :+: RawCommandError :+: CNil
  type RawMultipleItemCommandStatus = Map[String, RawItemStatus]
  type RawRequestStatus = Map[String, RawCommandStatus]

  case class RawCommandResult(SyncStatus: RawRequestStatus, TempIdMapping: Option[TempIdMapping])

  // synthetic DTOs
  case class CommandResult(status: RawCommandStatus)
  sealed trait TempIdCommandResult
  case class TempIdSuccess(realId: Int) extends TempIdCommandResult
  case class TempIdFailure(underlying: RawCommandError) extends TempIdCommandResult
}
