package ru.pavkin.todoist.api.core

import ru.pavkin.todoist.api.core.parser.{MultipleResourcesParser, SingleResourceParser}
import ru.pavkin.todoist.api.core.query.{MultipleQueryDefinition, SingleQueryDefinition}
import shapeless._

trait API[F[_], P[_], Base] {
  def get[R](implicit
             IR: HasRawRequest[R],
             parser: SingleResourceParser.Aux[P, Base, R]): SingleQueryDefinition[F, P, R, Base]

  def getAll[R <: HList](implicit
                         IR: HasRawRequest[R],
                         parser: MultipleResourcesParser.Aux[P, Base, R]): MultipleQueryDefinition[F, P, R, Base]
}






