package ru.pavkin.todoist.api.core.command

import cats.FlatMap
import ru.pavkin.todoist.api.core.decoder.{SingleCommandResponseDecoder, SingleResponseDecoder}
import ru.pavkin.todoist.api.core.{CommandReturns, ToRawRequest, RequestDefinition}
import shapeless._

trait MultipleCommandDefinition[F[_], P[_], C <: HList, R <: HList, Base]
  extends RequestDefinition[F, P, R, Base] {

  def and[CC, RR](command: CC)
                 (implicit
                  FM: FlatMap[P],
                  tr: ToRawRequest[CC],
                  cr: CommandReturns.Aux[CC, RR],
                  parser: SingleCommandResponseDecoder.Aux[P, CC, Base, RR])
  : MultipleCommandDefinition[F, P, CC :: C, RR :: R, Base]
}
