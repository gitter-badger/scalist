package ru.pavkin.todoist.api.core

import ru.pavkin.todoist.api.core.command.{MultipleCommandDefinition, SingleCommandDefinition}
import ru.pavkin.todoist.api.core.decoder.{MultipleResponseDecoder, SingleResponseDecoder}
import ru.pavkin.todoist.api.core.query.{MultipleQueryDefinition, SingleQueryDefinition}
import shapeless._

trait API[F[_], P[_], Base] {
  def get[R](implicit
             IR: HasRawRequest[R],
             parser: SingleResponseDecoder.Aux[P, Base, R]): SingleQueryDefinition[F, P, R, Base]

  def getAll[R <: HList](implicit
                         IR: HasRawRequest[R],
                         parser: MultipleResponseDecoder.Aux[P, Base, R]): MultipleQueryDefinition[F, P, R, Base]

  def perform[C, R](command: C)
                   (implicit
                    trr: ToRawRequest[C],
                    cr: CommandReturns.Aux[C, R],
                    parser: SingleResponseDecoder.Aux[P, Base, R]): SingleCommandDefinition[F, P, C, R, Base]

  def performAll[C <: HList, R <: HList](commands: C)
                                        (implicit
                                         trr: ToRawRequest[C],
                                         cr: CommandReturns.Aux[C, R],
                                         parser: MultipleResponseDecoder.Aux[P, Base, R])
  : MultipleCommandDefinition[F, P, C, R, Base]
}







