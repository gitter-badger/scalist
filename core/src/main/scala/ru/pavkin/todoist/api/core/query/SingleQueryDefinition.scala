package ru.pavkin.todoist.api.core.query

import cats.FlatMap
import ru.pavkin.todoist.api.core.{RequestDefinition, HasRawRequest}
import ru.pavkin.todoist.api.core.parser.SingleResourceParser
import shapeless.{::, <:!<, HNil}

trait SingleQueryDefinition[F[_], P[_], R, Base] extends RequestDefinition[F, P, R, Base] {

  def and[RR](implicit
              FM: FlatMap[P],
              NEQ: RR <:!< R,
              ir: HasRawRequest[RR],
              parser: SingleResourceParser.Aux[P, Base, RR])
  : MultipleQueryDefinition[F, P, RR :: R :: HNil, Base]
}
